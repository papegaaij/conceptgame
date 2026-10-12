---
title: Stinger
design: review
implementation: done
art: final
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-12
---

# Stinger

## Summary

A rust-red wasp-like diver: it enters, locks on and dives past the player, firing a fan at the bottom of its dive. Teaches dodging sideways.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis.

<!-- data: stat-block -->
| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `air` |
| Size tier | `small` |
| Size | 40×40 px, hitbox 24×30 |
| Parts | single |
| Orientation | `±30° tilt` (tilts towards its dive vector, up to ±30°) |
| HP | 6 (easy 4 / hard 8, from the global multipliers) |
| Armour / shield | none |
| Speed | 100 px/s entering; dive 420 px/s |
| Movement | `dive`: enter to y = 90–160 px, pause 0.5 s (crimson glow flares as telegraph), dive at the player's position, continue off the bottom edge |
| Attack | 3-way `fan` (spread 30°, centred on the player, 170 px/s, damage `small` = 4) when it passes the player's height or 0.6 s into the dive |
| Formations | V-wing (3–5), column (V-wing units dive one after another 0.4 s apart) |
| Weak points | glowing crimson abdomen (drawn only) |
| Effective traits | `forward`, `spread` |
| Credits | 15 (score 150 × chain) |
| Death | `small` burst: rust shards, crimson flash |
| First level / used in | L02; recurring diver through Acts 1–2 |
| Difficulty hooks | hard: fan 5-way; dive pause 0.4 s |
<!-- /data -->

### Behaviour

- The pause and glow flare are the telegraph; the dive vector is fixed at the moment the pause ends, so sidestepping works.
- A Stinger that passes the player without dying exits; it never returns.
- It tilts towards its dive vector, up to ±30°: an angle set of 7 headings (−30° to +30° in 10°
  steps around straight down), since a lit sprite is never rotated at runtime.
- The fan is centred on the player at the moment it fires.
- A Stinger entering from a side edge flies in along that edge's row to its pause height
  (90–160 px below the top edge), the units of a column spreading inwards from the edge, then
  pauses, glows and dives as usual.

- **Under the `always` hook** (Level 13, M5 part G, G1 = a; [rules](../../../campaign/act-2-homefront/level-13-polar-relay/README.md#what-hurts-the-relay-the-hook-modes-g1--a)): the dive is aimed at the
  **relay's centre** instead of the ship and the fan is centred on the relay. **Crossing the relay's
  footprint deals the spec's 10 once** (a diver's hit); the Stinger then flies on off the bottom edge: it
  stays shootable and does **not** die on the relay (not a kamikaze). The pause, glow and fixed dive
  vector are unchanged. On **easy** the Stingers are not hooked and dive at the ship; on hard Level 13's
  t=210 column dives in **pairs** (0.4 s between pairs, a wave override).

### Concept art

Chosen concept: [stinger-r04-a.png](../concept/stinger-r04-a.png) (listed in the [air](../README.md#concept-art) Concept art table).

## Concept art

Production art for concept round 15 (M4 part B, the Level 02 batch), review files built from the final files in `assets/` by `tools/art/vrell_air.py` (`--review` rebuilds only them); prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/stinger-final-r15-a.png](concept/stinger-final-r15-a.png) | Final sprites: the ±30° tilt set (7 headings, 10° apart, clockwise from straight down) × 4 wing-beat frames (40×40, 32 colours) and the additive crimson pause flare (3 frames) | chosen |
| [concept/stinger-final-r15-a.gif](concept/stinger-final-r15-a.gif) | A Stinger entering, pausing with its flare and diving tilted at 12 fps | chosen |

## Implementation

- [x] Dive with telegraph pause and locked vector
- [x] Fan fired at the bottom of the dive
- [x] Exits after the dive
- [x] Stat block values loaded from data; global difficulty multipliers applied
- [x] Death effect, bounty and score per this spec
- [x] `always` hook: dive and fan aimed at the relay, the single 10-point hit crossing its footprint, the exit afterwards; easy unhooked (M5 part G, steps G3 and G4, Level 13) — simulation done in G3 (`HookModesTest`; hard's pairs `WaveSpec.pairSeconds`, `CrossTest`); the level's table is G4's
- [x] Hard's pair interval in a column (a wave override) (M5 part G, G3 and G4)

## Decisions

- 2026-10-01: Promoted from the air roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-02: M4 part B (user decisions): the tilt is a 7-heading angle set (±30° in 10° steps); the fan is centred on the player; side-entering Stingers fly in along the edge to their pause height, spread inwards, then dive; the weak point is drawn only (single-part rule of [enemies](../../README.md)).
- 2026-10-02: M4 part B: built (`EnemySpec.Dive`: its pause is the hold, the dive locks onto where the ship was; one fan when it passes the ship's height or 0.6 s into the dive, never closer than 72 px to the ship, so a Stinger flown at head-on rams instead; the `single` and `column` formations), its production sprites proposed in [concept round 15](../../../concept-rounds/round-15/README.md).
- 2026-10-02: Concept round 15 closed (user decision): the production sprites approved as **final**, `art: final`.
- 2026-10-11: M5 part G, step G1a (the user is on vacation, the main agent took the gap analysis's recommendations; **pending the user's review** in the combined round at the end of M5): **G1 = a** the `always` hook made exact: the dive and fan aimed at the relay, 10 once crossing its footprint, then it flies on (rejected for review: dying on the relay as a kamikaze); easy unhooked, hard's pairs. `design: review` for this behaviour, `implementation: in-progress` for its build.
- 2026-10-11: M5 part G, step G3 (the simulation; pending the user's review): under `always` the fan keeps its stat block's timing (it passes the player's height or 0.6 s into the dive, whichever comes first), so in Level 13 it fires from below the relay back up at it and the player can body-block it (rejected: firing as it crosses the relay, which nobody could block); the dive's 10 is dealt once as its hit box crosses the relay's 80×80 hit box. While the relay cannot be aimed at (before it scrolls in, after the synchronisation) a hooked Stinger dives at the ship. Hard's pairs fly side by side 50 px apart.
- 2026-10-12: M5 part I (close-out): the `always` hook and hard's pairs ticked as built in part G (Level 13's data: `targeted_by`, the t=210 column's `hard: {pairs: 0.4}`; `HookModesTest`, `CrossTest`, `Level13Test`); every item is ticked, so the document is `done`.
