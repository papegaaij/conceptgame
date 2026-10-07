---
title: Hive Node
design: approved
implementation: done
art: final
depends-on: [../../../systems/difficulty, ../../../systems/economy, ../../air/skitter]
updated: 2026-10-07
---

# Hive Node

## Summary

A hardened teal-black spawner mound grown into the ruins. Its iris opens every few seconds to release Skitters until it is destroyed, and only `anti-ground` weapons, the Airstrike and the Smart Bomb can hurt it. Level 09's six named targets.

## Design

### Stat block

The numbers live in [data.yaml](data.yaml). Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis. Its HP is set at its first level (L09, reference DPS 63: time-to-kill 1.0 s); where it returns (L14) it gets the act HP factor of a `medium` unit. The spawn is built (M5 part C) and read from the data file.

<!-- data: stat-block -->
| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `ground` |
| Size tier | `medium` |
| Size | 76×76 px, hitbox 56×56 |
| Parts | single |
| Orientation | `radial` (no headings; the lobes pulse, the iris opens and shuts) |
| HP | 64 (easy 48 / hard 83, from the global multipliers) |
| Armour / shield | hardened |
| Speed | 0 px/s (scrolls with the ground) |
| Movement | `terrain` |
| Attack | `spawn`: its iris opens over 0.5 s every 4 s and releases 2 Skitters, flying out at 160 px/s in a 120° arc toward the ship; it stays shut while the ship is within 96 px; no direct fire |
| Formations | single (placed in pairs, each node its own named target) |
| Weak points | open iris (drawn only; it glows while the iris opens, the spawn's telegraph) |
| Effective traits | `anti-ground` |
| Credits | 45 (score 450 × chain); the Skitters it releases pay their own 5 and count into the level's enemies as released |
| Death | `large` organic collapse; the creep around it withers over 2 s |
| First level / used in | L09; the primary `destroy-targets` objective's named targets; returns L14 (with the act HP factor) |
| Difficulty hooks | hard: 3 Skitters per opening |
<!-- /data -->

### Behaviour

- **Hardened** (M5 part C default, flown for an enemy as for a hardened ground object): only an
  `anti-ground` delivery (the Bomb Rack's bombs, the Hammer Mortar's shells, Rook's Mortar, an
  `anti-ground` bolt), the Airstrike and the Smart Bomb damage it. Every other shot or blast
  glances off with a spark and the glance sound (`SHOT_GLANCED`), which teaches the rule on the
  first hit, as Level 09's briefing does. Homing shots and Rook do not pick it as a target unless
  their weapon is `anti-ground`, so they do not waste themselves on it.
- **Spawn cycle.** Every 4 s, counted from when its centre crosses the top edge, the iris opens
  over 0.5 s (the telegraph: the iris glows, the [Vrell spawn](../../../audio/sfx/README.md#enemies)
  swells) and releases 2 Skitters (hard 3) out of the throat. They fly straight out at 160 px/s
  in a 120° arc centred on the ship, as a [Brood Pod](../../air/brood-pod/README.md)'s brood does,
  then fly on as [Skitters](../../air/skitter/README.md). The iris then shuts. An opening that is
  due while the ship's centre is within **96 px** of the node's is skipped (the iris stays shut,
  no telegraph), so no Skitter appears on top of the ship (the 72 px bullet rule, with room for a
  Skitter's size); the next one is due 4 s later. The cycle runs while the node lives, on the
  screen or above it; it stops at its death.
- **Released Skitters** pay their own bounty (5, Act 1 terms) and count into the level's enemies
  as they are released, as a boss's streams do. The density and typical-haul checks
  (`DensityTest`, `TypicalHaul`) count **one opening per node** (2 Skitters at medium), the
  budget's assumption that a typical player kills a node after about one opening.
- **Weak point drawn only** (single-part rule, M5 part C default as for the Creeper): the open
  iris glows, but takes no extra damage; the draft's "×2 while open" is struck.
- **Placement.** A node is a ground unit placed by a level's ground targets, each node its own
  group (Level 09: `Node A1` … `Node C2`, two per cluster), so a `destroy-targets` primary can name
  it; it never walks. It stands on the ground plane only (a plaza, a bridge approach, a lobby
  plaza), never on a perspective tower ([art direction](../../../art-direction/README.md#parallax-layer-model)).
- **Death.** The `large` organic collapse, then the creep patch around it withers over 2 s (drawn
  by the game from its frames).

### Concept art

Chosen concept: [hive-node-r06-a.png](../concept/hive-node-r06-a.png), [hive-node-r06-a.gif](../concept/hive-node-r06-a.gif) (listed in the [ground](../README.md#concept-art) Concept art table). M5 part C takes it straight to production (user decision D9 = a): the radial 76×76 sprite with its lobe pulse and the iris opening (0–1), the creep patch and its 2 s wither, the `large` death and a 30×30 intel portrait, approved as final in concept round 31 (user, 2026-10-07).

The production files, generator notes in [concept/prompts.md](concept/prompts.md):

| File | What | Status |
|---|---|---|
| [concept/hive-node-final-r31-a.png](concept/hive-node-final-r31-a.png) | Final sprites (`tools/art/hive_node.py`): 6 iris states × 4 pulse frames (`hive-node_0..23`, 76×76), glow masks with the iris halo (`hive-node-glow_0..23`), the creep and its 2 s wither (`hive-node-creep_0..8`, 192×192), the stump, the 16-frame death (128×128) alone and with the `large` burst | chosen |
| [concept/hive-node-final-r31-a.gif](concept/hive-node-final-r31-a.gif) | The node in its creep on Level 08's night avenues opening twice 4 s apart, two Skitters out each time; destroyed, the creep withers, the stump stays | chosen |

## Implementation

- [x] Stat block values loaded from [data.yaml](data.yaml); global difficulty multipliers applied
- [x] Hardened for an enemy: the glance and spark for shots and blasts without `anti-ground`; the Airstrike and the Smart Bomb hit it; homing shots and Rook skip it unless anti-ground
- [x] Periodic spawn: every 4 s, 0.5 s telegraph, 2 Skitters (hard 3) in a 120° arc toward the ship, shut while the ship is within 96 px; the iris state for the renderer; released units counted into the level's enemies
- [x] `DensityTest` and `TypicalHaul` count one opening per node
- [x] Creep wither on death
- [x] Death effect, bounty and score per this spec
- [x] Production sprites (lobe pulse, iris 0–1, creep patch and wither, death) and intel portrait (art track, concept round 31)

## Open questions

- None open.

## Decisions

- 2026-10-01: Promoted from the ground roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: HP rescaled to the lowered L09 reference DPS (80 → 64, ×63/80); time-to-kill stays ≈ 1.0 s.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-07: M5 part C (user decisions D1–D11 of 2026-10-07 and the stated defaults): the numbers
  moved into [data.yaml](data.yaml) and the table is rendered from it; the weak point is **drawn
  only** (single-part rule), so the iris's ×2 is struck; **hardened** flies for an enemy (the
  glance, the Airstrike and Smart Bomb hit it, homing shots and Rook skip it unless anti-ground);
  the **spawn** every 4 s with a 0.5 s telegraph, 2 Skitters (hard 3) in an arc toward the ship
  like a brood, the iris shut while the ship is within 96 px, the released Skitters counted as
  released and one opening per node in the density and haul checks. The formations row reads
  `single` (each node its own named target, placed in pairs) instead of "turret nest (2–4)",
  which the vocabulary defines as 3–6 turrets. Our readings, for review in round 31: the cycle
  counted from the top edge (first release 4 s after entering), a skipped opening waits a whole
  cycle, the 120° arc and 160 px/s of the Brood Pod. The spawn's keys are planned (the
  [schemas](../../../tech/architecture/README.md#data-file-schemas)): they wait in the data's
  comment until the simulation reads them. `design` goes to `review` for round 31.
- 2026-10-07: M5 part C simulation built: the spawn and its keys are read from the data file (no longer a comment); the simulation items are ticked, the drawing and the art follow.
- 2026-10-07: [Concept round 31](../../../concept-rounds/round-31/README.md) closed for the Hive
  Node (user, 2026-10-07): the production sprites (`hive-node_0..23` with the iris and the lobe
  pulse, the glow masks, the creep and its wither, the `large` death with its tatters, the stump)
  and the intel portrait approved as **final**, the weak spots as they are (night contrast 3.5, the
  stump, the static creep); the reuse of the Brood Carrier's iris b and the Vrell spawn a, and no
  screech, confirmed; the stat block's numbers and our readings (the cycle from the top edge, a
  skipped opening waiting a whole cycle, the Brood Pod's arc and speed) accepted with the part C
  numbers. `design: approved`, `art: final`; every Implementation item is ticked, so the
  implementation is `done`.
