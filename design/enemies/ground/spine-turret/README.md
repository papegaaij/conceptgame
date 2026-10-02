---
title: Spine Turret
design: approved
implementation: done
art: chosen
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-02
---

# Spine Turret

## Summary

A grown Vrell turret: a slate bulb with a petal collar and a thorn barrel that rotates to track the player. The basic ground threat.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis.

<!-- data: stat-block -->
| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `ground` |
| Size tier | `small` |
| Size | 40×40 px, hitbox 30×30 |
| Parts | single (barrel rotates) |
| Orientation | `32 angles` (the barrel; the body is fixed and faces down the screen) |
| HP | 8 (easy 6 / hard 10, from the global multipliers) |
| Armour / shield | none |
| Speed | 0 px/s (scrolls with the ground) |
| Movement | `terrain` |
| Attack | `aimed` standard orb every 2 s along its barrel (turning 90°/s), 160 px/s, damage `small` = 4; silent once the player is more than 100° off its facing (behind it) |
| Formations | turret nest (3–6) |
| Weak points | violet barrel root (drawn only) |
| Effective traits | `anti-ground`, `forward` |
| Credits | 12 (score 120 × chain) |
| Death | `small` organic crumble; leaves a scorched stump on the ground layer |
| First level / used in | L02; recurring ground fodder in Acts 1–2 (incl. Luna and Vrell creep) |
| Difficulty hooks | hard: 2-shot bursts |
<!-- /data -->

### Behaviour

- Ground layer: no collision with the player; hit by all weapons, ×2 from `anti-ground`.
- It faces down the screen, towards where the player comes from. Its barrel turns towards the
  player at 90°/s and it fires along the barrel; once the player is more than 100° off its facing
  (above and behind it, after the ground has carried it past), it stops turning and firing, so a
  turret that has been passed is harmless (Varga's tip in Level 02).

### Concept art

Chosen concept: [spine-turret-r04-a.png](../concept/spine-turret-r04-a.png) (listed in the [ground](../README.md#concept-art) Concept art table).

## Concept art

Production art for concept round 15 (M4 part B, the Level 02 batch), review files built from the final files in `assets/` by `tools/art/vrell_ground.py` (`--review` rebuilds only them); prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/spine-turret-final-r15-a.png](concept/spine-turret-final-r15-a.png) | Final sprites: the whole turret at 32 barrel headings (40×40, 32 colours; heading 0 aims down, 8 left, 24 right), each its own render under the fixed key light, and the scorched stump | proposed |
| [concept/spine-turret-final-r15-a.gif](concept/spine-turret-final-r15-a.gif) | The barrel tracking a ship sweeping past below it | proposed |

## Implementation

- [x] Barrel tracking at 90°/s with 32 frames
- [x] Aimed shots (standard orbs)
- [x] Stump decal on death
- [x] Stat block values loaded from data; global difficulty multipliers applied
- [x] Death effect, bounty and score per this spec

## Decisions

- 2026-10-01: Promoted from the ground roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-02: Fires the standard orb instead of the yellow thorn (user decision): at 160 px/s it is in the standard speed class, and the yellow needle is reserved for the fast class.
- 2026-10-02: M4 part B (user decisions): its facing is straight down the screen; the barrel tracks at 90°/s and the shots leave along it; beyond 100° off its facing it is silent; the weak point is drawn only (single-part rule of [enemies](../../README.md)).
- 2026-10-02: M4 part B: built (a ground unit of the level's ground targets, scrolling with the ground; the barrel turns 90°/s inside its 100° arc and fires along itself; its bounty pays as a ground target; the stump stays on the ground until it scrolls off), its production sprites proposed in [concept round 15](../../../concept-rounds/round-15/README.md).
