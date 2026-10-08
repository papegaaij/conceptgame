---
title: Wraith
design: approved
implementation: done
art: final
depends-on: [../../../systems/difficulty, ../../../systems/economy, ../../../allies]
updated: 2026-10-08
---

# Wraith

## Summary

A rust-and-blue-violet Vrell manta ghost that passes overhead cloaked, loops behind the player and decloaks to attack up the screen. The signature rear attacker of Act 2.

## Design

### Stat block

The numbers live in [data.yaml](data.yaml). Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis. Its HP is set for the balance plan's **rear** firepower, not the reference DPS (user decision D7 = a of M5 part D, see *Behaviour*). The cloak, the ambush path and the hard hold are planned keys (the [schemas](../../../tech/architecture/README.md#data-file-schemas)): they wait in the data's comment until the simulation reads them.

<!-- data: stat-block -->
| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `high-air` while cloaked (only `homing` hits it, no contact, specials skip it) → `air` from its decloak flash on |
| Size tier | `medium` |
| Size | 72×72 px, hitbox 50×40 |
| Parts | single |
| Orientation | `16 angles` (16 headings × 4 ripple frames; a cloaked shimmer set) |
| HP | 16 (easy 12 / hard 21, from the global multipliers) |
| Armour / shield | none |
| Speed | 200 px/s (cloaked, the swoop and the re-entry; 120 px/s decloaked, its exit) |
| Movement | cloaked `swoop` straight down its lane past the ship and off the bottom edge, 1.5 s below it, `rear-entry` up to y = 470–520 px (20–70 px above the bottom edge; the edge warning 3 s ahead of the re-entry, while it still swoops), decloak (a 0.4 s violet flash, on `air` from its start), `hover` 2.5 s, then out up the nearer side lane (its centre 40 px from that edge) decloaked at 120 px/s, clear of the shuttle band |
| Attack | 5-shot `burst` (0.12 s apart, 220 px/s, `medium` = 6) straight up the screen as a fixed 40° fan, its shots in turn from left to right, aimed at nobody and fired however close the player is (no no-fire distance), twice in its hold (0.9 s after it stops, then 1.2 s later); only while decloaked |
| Formations | rear ambush (1–4) (2–4 across the bottom edge, each in its own lane; a lone one as its introduction) |
| Weak points | blue-violet veins (drawn only) |
| Effective traits | `rear`, `homing` |
| Credits | 30 (score 300 × chain) |
| Death | `medium` burst: membrane tatters, violet flash |
| First level / used in | L10; L12, L14 (with the act HP factor); launched by the Siege Spire (L14) |
| Difficulty hooks | hard: 7-shot bursts; holds 3.0 s |
<!-- /data -->

### Behaviour

User decision D6 = a of M5 part D: it flies as its stat block says.

- **Cloaked** (`high-air`, the stat block's layer): it enters at the top edge at the wave's time and
  flies straight down its lane at 200 px/s, past the ship and off the bottom edge. While cloaked it
  is drawn as a refraction **shimmer** at the high-air scale (attentive players see it coming
  before the warning); only `homing` shots hit it (there is no player `beam` weapon in Act 2), it
  never touches the ship, Rook or a shuttle, and the specials skip it, as every `high-air` unit.
- **Rear entry.** 1.5 s below the bottom edge, it comes back up to its hold point, y = 470–520 px
  (20–70 px above the bottom edge), still cloaked. The bottom edge is **warned 3 s ahead of the
  re-entry** (so the warning starts while it still swoops, as a chain's loop-back), with Rook's
  rear bark ([wingmen](../../../player/wingmen/README.md#radio-barks)); its wave counts as a `rear`
  wave for Rook's Trail from the re-entry.
- **Decloak.** At its hold point it decloaks: a **0.4 s violet flash**, on `air` from the flash's
  start (the unit's current layer, as the Ravager's pounce; every weapon hits it, it has contact),
  its gun silent until the flash ends. The simulation raises a decloak event for the flash, its
  sound and the radio event `first-decloak`.
- **Hold and bursts.** It holds **2.5 s** (hard 3.0 s) after the flash and fires two 5-shot bursts
  (hard 7) **straight up the screen as a fixed 40° fan**, 0.5 s and 1.7 s into the hold: each
  burst's shots go in turn from the fan's left edge to its right, aimed at nobody, so in an escort
  level they rake the shuttle band above it and hurt a shuttle they touch
  ([allies](../../../allies/README.md#evacuation-shuttle)); a ship flying between the Wraith and the
  band takes them instead. The 72 px no-fire distance does not apply to these bursts (they are not
  aimed at the ship, so a ship parked on top of a holding Wraith is hit as well).
- **Exit.** It leaves **decloaked** up the nearer **side lane** (its centre 40 px from that edge; a
  tie goes left) at 120 px/s, clear of the shuttle band (Level 10's stations stay between x = 120
  and 360), so forward guns and Rook get a second chance at it.
- **Time to kill** (D7 = a): 16 HP (easy 12, hard 21). The plan's L10 rear DPS is 10 (the Tail Gun
  L1): 1.6 s of its 2.9 s decloak-and-hold window. `BalanceTest` gets a **rear check**: HP ÷ the
  plan's rear DPS at most 0.6 × (flash + hold) at medium. At the L10 reference DPS 66 it dies in
  0.24 s, below the `medium` class's 0.4–1.5 s: an accepted exception like the Ravager's (homing
  and forward guns reach it only cloaked or on its exit). Where it returns (L12, L14) it gets the
  act HP factor of a `medium` unit.
- **Weak point drawn only** (single-part rule, as part C did for the Hive Node and the Ravager):
  the blue-violet veins glow but take no extra damage; the draft's ×1.5 is struck.
- Its colours are rust chitin with blue-violet veins everywhere (user decision, round 06),
  including when the Siege Spire launches it.

### Concept art

Chosen concept: [wraith-r06-a.png](../concept/wraith-r06-a.png), [wraith-r06-a.gif](../concept/wraith-r06-a.gif) (listed in the [air](../README.md#concept-art) Concept art table). M5 part D takes it straight to production (user decision D10 = a, concept round 32): 72×72, 16 headings × 4 ripple frames, the cloaked shimmer set, the decloak flash, the `medium` death with its membrane tatters and an intel portrait.

The production files (M5 part D, straight to production per D10 = a; approved as final in concept round 32), generator notes in [concept/prompts.md](concept/prompts.md):

| File | What | Status |
|---|---|---|
| [concept/wraith-final-r32-a.png](concept/wraith-final-r32-a.png) | Final sprites (`tools/art/wraith.py`): 16 headings × 4 ripple frames (`wraith_0..63`, 72×72), the cloak shimmer (`wraith-cloak_0..63`, 90×90, additive), the 0.4 s decloak flash (`wraith-decloak_0..5`), the `medium` death and tatters (96×96), the decloak as drawn | chosen |
| [concept/wraith-final-r32-a.gif](concept/wraith-final-r32-a.gif) | The cloaked swoop, the rear warning, the rise, the decloak, two bursts, the side-lane exit, the death | chosen |

## Implementation

- [x] Stat block values loaded from [data.yaml](data.yaml); global difficulty multipliers applied
- [x] Cloak: `high-air` until the decloak, `air` from the 0.4 s flash's start (the unit's current
      layer at every hit, contact and targeting site), the gun only after the flash; the decloak
      event (D6 = a)
- [x] The `rear ambush` path: cloaked straight down its lane at 200 px/s, 1.5 s below the bottom
      edge, up to y 470–520 px; the bottom-edge warning 3 s ahead of the re-entry; Rook's Trail and
      rear bark from it; the 2.5 s hold (hard 3.0 s); out up the nearer side lane at 120 px/s
- [x] Two bursts in its hold, 5 shots (hard 7) 0.12 s apart, 220 px/s, `medium`, straight up as
      a fixed 40° fan (`aim: up`), not aimed and without the no-fire distance
- [x] HP 16 (D7 = a) and `BalanceTest`'s rear check; its time to kill at L10's reference an
      accepted exception
- [x] Death effect, bounty and score per this spec
- [x] Production sprites (headings and ripple frames, shimmer, flash, death) and intel portrait;
      the decloak sound (concept round 32's b)

## Decisions

- 2026-10-01: Promoted from the air roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: HP rescaled to the lowered L10 reference DPS (36 → 27, ×66/90, rounded up to keep the 0.4 s medium minimum); time-to-kill ≈ 0.41 s.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-08: M5 part D (user decisions of 2026-10-08): **D6 = a** it flies as its stat block:
  enters cloaked at the top on `high-air` (homing only, no contact, specials skip it), swoops down
  past the ship and off the bottom edge, re-enters after the 3 s warning to 20–70 px above the
  edge, decloaks with a 0.4 s flash (on `air` from its start), holds 2.5 s with two bursts, then
  leaves decloaked up a side lane clear of the shuttle band; rejected: b (re-cloaking before it
  leaves: the hold the only kill window) and c (entering straight from the bottom, losing the pass
  overhead and the homing window). **D7 = a** HP 27 → **16** (hard ≈ 21) for the plan's rear DPS 10,
  a rear check in `BalanceTest`, the plan's L10 visit refitting Rook's Autocannon; rejected: b
  (27, the exit the main kill) and c (27 with the Tail Gun L2 at L10, still over the window's 0.6).
  Stated defaults: the veins' ×1.5 struck (drawn only), the 1.5 s warning replaced by the 3 s rule
  (warned ahead of the re-entry), the edge warning's call now Rook's rear bark, the hard hold and
  burst as its stat block, the numbers in [data.yaml](data.yaml). Our readings, for review in round
  32: straight down its own lane (no aimed swoop); the lanes of a `rear ambush` evenly spread across
  the bottom edge; the bursts 0.5 s and 1.7 s into the hold; the exit lane 40 px from the nearer
  edge. `design` goes to `review` for round 32.
- 2026-10-08: M5 part D simulation built (the movement, the cloak or the flock, the formations and
  their keys, read from the data file); its simulation items are ticked, the drawing, the sounds and
  the art follow.
- 2026-10-08: **Bursts straight up (user):** the capture showed bursts aimed at the ship rarely
  crossing the shuttle band, and a Wraith holding within 72 px of the ship was silenced by the
  no-fire rule in most holds. The two bursts now go straight up the screen as a fixed fan
  (`aim: up`, `spread: 40`° from the first shot on the left to the last on the right), through the
  shuttle band, not at the ship, and the no-fire distance does not apply to them; the shot count,
  gap, speed and timing are unchanged.
- 2026-10-08: Concept round 32 closed (user): the production art (`wraith_0..63`, the cloak shimmer,
  the decloak flash, the `medium` death and tatters) and the intel portrait approved as **final**,
  the weak spots as they are (the flash's blobby frame 1, the stick-like tatters, the faint cloak
  over the city: build choice 21d kept); the decloak sound **b** (a metallic rip over a membrane
  swish, CC-BY: F.M.Audio on the credits roll); the stat block's numbers and the build choices of
  round 32 (the two bursts, the exit lane, the hold depth, the `rear ambush`, the `Trig` table)
  accepted. `design: approved`, `art: final`; every Implementation item is ticked, so the
  implementation is `done`.
