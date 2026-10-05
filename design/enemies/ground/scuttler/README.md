---
title: Scuttler
design: approved
implementation: done
art: final
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-05
---

# Scuttler

## Summary

A slate-and-lime six-legged crab walker that strides across the terrain on its own heading, turning to face where it goes and firing fans in its facing direction. Claws armoured from the front, glowing back exposed.

## Design

### Stat block

The numbers live in [data.yaml](data.yaml). The table is still hand-written: `tools/sync_tables.py` prints the armour's `front_arc` mapping raw, so the table stays unmarked until the renderer learns it. Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis.

| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `ground` (sea floor in Act 4) |
| Size tier | `medium` |
| Size | 64×64 px, hitbox 46×40 |
| Parts | single, with armoured claws |
| Orientation | `16 angles` (16 headings × 6 walk frames; it faces where it walks) |
| HP | 28 (easy 21 / hard 36, from the global multipliers) |
| Armour / shield | claws `armoured` from the front: direct shots arriving from within ±45° of its facing spark off; dropped bombs, lobbed shells and the Airstrike ignore it |
| Speed | 40 px/s walking (plus scroll) |
| Movement | `walk` along an authored ground path; turns at 90°/s; the walk phase advances with distance (24 px per cycle) |
| Attack | 5-way `fan` in its facing direction every 2.8 s (spread 60°, 140 px/s, `small` = 4); acid spit (`aimed`, 120 px/s, `medium` = 6) every 4 s while facing away from the player (more than 90° off its facing) |
| Formations | single, convoy, pincer (walking in from both sides) |
| Weak points | glowing lime back (drawn only) |
| Effective traits | `anti-ground` (×2), `side`, `homing` |
| Credits | 25 (score 250 × chain) |
| Death | `medium` organic burst: shell fragments, legs scatter; leaves a legless husk at its last heading |
| First level / used in | L04; returns L13 and on Europa's sea floor (L24) |
| Difficulty hooks | hard: fan 7-way |

### Behaviour

- Walks can cross the player's lane, so the back becomes exposed as it turns: rewards patience or side weapons.
- **Walk.** A wave gives each Scuttler a path of `[x, y]` points in screen coordinates at the
  wave's time; the path then scrolls with the ground, and the Scuttler walks along it at 40 px/s
  on top of the scroll. Its facing is the direction it walks over the ground, turning at most
  90°/s; the nearest of the 16 headings is drawn. The facing is simulation state (it decides the
  armour and the fan), so it goes into the state hash. The walk phase advances with the distance
  walked: one 6-frame cycle per 24 px of ground.
- **Frontal armour.** A direct shot (it flies along the player's plane: bullets, homing missiles,
  beams) whose direction of arrival lies within ±45° of the facing sparks off and does no damage.
  Dropped bombs, lobbed shells and the Airstrike's blasts come from above and ignore the armour
  (user decision); they still get the `anti-ground` ×2 of the layer rules.
- **Convoys** (Level 04): its facing `fan` is not an aimed attack, so it never hurts a crawler;
  the acid spit goes at whichever is nearer under the target-the-objective hook, and its claws
  hurt a crawler they overlap (see the [civilian crawler](../../../allies/README.md#civilian-crawler)).
- **Death.** A 16-frame death burst, then a legless husk at its last heading (16 husk frames)
  that scrolls away with the ground.

### Concept art

Chosen concept: [scuttler-r08-a.png](../concept/scuttler-r08-a.png), [scuttler-r08-a.gif](../concept/scuttler-r08-a.gif) (the round-08 re-render; r05 is superseded) (listed in the [ground](../README.md#concept-art) Concept art table).

## Concept art

Production art proposal for concept round 16 (M4 part D, the Level 04 batch; pending part D's doc gaps), from the chosen round-08 re-render (`scuttler-r08-a`); review files built from the files in `assets/` by `tools/art/vrell_l04.py` (`--review scuttler` rebuilds only them); prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/scuttler-final-r16-a.png](concept/scuttler-final-r16-a.png) | Final sprites: 16 headings × 6 walk frames (`scuttler_0..95`, 64×64, 32 colours, 24 px per cycle), the walk cycle at three headings, the legless husks (`scuttler-husk_0..15`) and the additive lime-back glow masks (`scuttler-glow_0..95`) | chosen |
| [concept/scuttler-final-r16-a.gif](concept/scuttler-final-r16-a.gif) | A Scuttler walking a circle under a dust bank, its back glow added on top | chosen |
| [concept/scuttler-death-final-r26-a.png](concept/scuttler-death-final-r26-a.png) | Round 26 (M4 part H, `tools/art/vrell_deaths.py`): the 16-frame organic burst, `scuttler-tatters_0..15` (96×96, solid: the six legs and the claws torn off, shell fragments off the back, settling on the ground) and `scuttler-death_0..15` (96×96, additive lime flash, ichor spray and olive mist), with the medium burst, over its husk and at 1× | proposed |
| [concept/scuttler-death-final-r26-a.gif](concept/scuttler-death-final-r26-a.gif) | Round 26: three Scuttlers dying and leaving their husks on Luna grey | proposed |

## Implementation

- [x] Walker path with turn rate and distance-driven walk phase
- [x] Frontal claw armour: direct shots glance from ±45°; bombs, shells and the Airstrike ignore it
- [x] Walk path in screen coordinates scrolling with the ground; facing in the state hash
- [x] Facing fan
- [x] Stat block values loaded from data; global difficulty multipliers applied
- [x] Bounty and score per this spec; the husk at its last heading
- [x] Death effect: the 16-frame organic burst (production frames proposed in round 26), with the medium explosion

## Decisions

- 2026-10-01: Promoted from the ground roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-03: Concept round 16 closed (user decision): the production sprites (16 headings × 6 walk frames, the husks, the back-glow masks) approved as **final**, `art: final`, ahead of M4 part D. Part D's doc gaps may still require changes to them; those go through a later round.
- 2026-10-03: Bombs, shells and the Airstrike ignore the frontal armour; only direct shots glance
  (user decision). Counting homing missiles and beams as direct shots is our reading.
- 2026-10-03: Part D doc gaps settled (main-agent choices following earlier decisions): the
  numbers moved into [data.yaml](data.yaml) (`walk` movement, the armour's `front_arc`, the fan's
  `aim: facing`, the spit's `away`); the weak point is drawn only (single-part rule), so the ×2 is
  gone; `single` added to the formations (the Level 04 introduction); the chosen concept is r08
  (the text still named r05). The walk path is in screen coordinates and scrolls with the ground,
  the facing goes into the state hash, one walk cycle per 24 px, a 16-frame death burst and a
  16-heading husk. Chosen here: "facing away from the player" read as more than 90° off its facing.
- 2026-10-03: M4 part D step 3, built: a wave's `paths` (one per unit, in screen points at the
  wave's t that then scroll with the ground) walked at 40 px/s over the ground, the facing turning
  at most 90°/s toward the next point (a point it reaches within 6 px, or one behind it inside its
  turning circle, is passed) and straight on after the last; the walker formations separate from the
  air ones (`pincer`: a single path mirrored about x = 240 for every second unit, each further pair
  1.5 s later; `convoy` and `single`: the path repeated 1.5 s apart); the facing, the walked distance
  and the spit timer in the state hash (only for walkers, so Levels 01–03 hash as before); the
  frontal armour (a direct shot arriving within ±45° of the facing glances, bombs, shells and the
  Airstrike ignore it); the fan along the facing and the spit only with the ship more than 90° off
  it (each first at half its interval after it walks onto the screen, counting only on screen); the
  claws as any walking ground unit. Drawn: 16 headings × 6 walk frames, one cycle per 24 px walked,
  the lime back glow additive above the low-air layer, the husk at its last heading left on the
  ground (the `-husk` frames are its remains for a walker, not death pieces).
- 2026-10-05: M4 part H (round 26 batch): the 16-frame organic burst rendered by `tools/art/vrell_deaths.py` (legs and claws torn off and shell fragments scattering, a lime flash, ichor spray and olive mist; 4 steps a frame with the medium burst) and played by the game by name (`scuttler-death`, `scuttler-tatters`; the husk stays the remains); review files proposed for [round 26](../../../concept-rounds/README.md), `art` unchanged until the user approves them. Seen in play: the burst and pieces are drawn in screen space while the husk scrolls with the ground (120 px/s in Level 04), so the pieces drift up off the husk over the burst's second; drawing a ground unit's death in the ground's scroll is a LevelScreen hook.
