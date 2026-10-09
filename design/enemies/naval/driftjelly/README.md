---
title: Driftjelly
design: approved
implementation: done
art: final
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-09
---

# Driftjelly

## Summary

An olive jellyfish mine with lime veins that drifts on or below the surface and pulses a ring of shots when the player comes close.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis. The numbers live in [data.yaml](data.yaml) (M5 part E, step E2b), with the swap timer (`submerge`), the proximity ring's reach (`within`) and the `field` formation of the [schemas](../../../tech/architecture/README.md#data-file-schemas); the table is rendered from it.

<!-- data: stat-block -->
| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `ground` while surfaced, `sub` while submerged (its **current layer**, flipping at the middle of each swap) |
| Size tier | `small` |
| Size | 40×40 px, hitbox 28×28 |
| Parts | single |
| Orientation | `radial` (4 pulse frames) |
| HP | 4 (easy 3 / hard 5, from the global multipliers) |
| Armour / shield | submerged: only `anti-sub` (the torpedo) and the Smart Bomb reach it ([layer rules](../../README.md#layer-rules)) |
| Speed | 15 px/s drift on the current, on top of the sea's scroll |
| Movement | `drift` with the sea (scroll factor 1, surfaced or not) along its wave's current; surfaces and submerges on its own seeded timer, every 6–10 s, each swap taking 0.6 s (the water rules: crown first, foam collar, ripples) |
| Attack | 8-bullet `ring` (90 px/s, `small` = 4) when the player is within 96 px; cooldown 2.5 s; surfaced **and** submerged |
| Formations | field (6–12) (scattered fields) |
| Weak points | lime bell veins (drawn only) |
| Effective traits | `spread`, `anti-sub` |
| Credits | 10 (score 100 × chain) |
| Death | `small` wet pop; on water a splash and ripple train |
| First level / used in | L11; Act 2 oceans; Europa (Act 4) |
| Difficulty hooks | hard: 12-bullet ring, radius 110 px |
<!-- /data -->

### Behaviour

- **Field** (default of M5 part E): a jelly wave is a `field`, units scattered over an area that
  enters at the top edge with the sea and drifts on the wave's current at 15 px/s (the
  [formation vocabulary](../../README.md#formation-vocabulary)). Submerged jellies scroll with the
  sea like surfaced ones (factor 1, not the 0.8–0.9 of sea-floor scenery).
- **Surface and submerge** (default): each jelly has its own deterministic timer, seeded per unit
  and run on the simulation's real steps (so it keeps going in a halted arena), drawing the next
  swap 6–10 s ahead; a field starts with about half its jellies submerged. A swap takes ≈ 0.6 s,
  drawn top-down per the [water rules](../../../art-direction/README.md#animation-rules) (the crown
  breaks the surface first, the foam collar grows, water streams off), and the layer flips at its
  midpoint: from then every hit, contact and targeting site reads the new layer (the part C/D
  current layer).
- **Who reaches it**: surfaced, it is a naval `ground` target, hit by every weapon (bombs and mortar
  shells included); submerged, only a torpedo or the Smart Bomb. Homing shots and Rook never pick a
  submerged jelly.
- **The ring** (user decision E3 = b): it fires surfaced **and** submerged; the bullets are on the
  player's plane either way. The 96 px trigger (hard 110) is measured from the ship's centre and
  the 2.5 s cooldown runs from the last ring. In game the warning is the bell's **quickening
  pulse**, which shows through the water when it is submerged (it quickens from 1.5 × the trigger
  radius, a first value); the 96 px circle is a diagram on the concept sheet only.
- **Weak point drawn only** ([single-part rule](../../README.md#stat-block-template)): the lime
  veins glow but take no extra damage; the draft's ×1.5 is struck.
- `area` is struck from its effective traits: the mines' blasts are on the player's plane and miss
  `ground`; the Hammer Mortar's blast hits a surfaced jelly as any `ground` unit, never a submerged
  one (E2 = a).

### Concept art

Chosen concept: [driftjelly-r07-a.png](../concept/driftjelly-r07-a.png), [driftjelly-r07-a.gif](../concept/driftjelly-r07-a.gif) (listed in the [naval](../README.md#concept-art) Concept art table). M5 part E takes it straight to production (user decision E9 = a, concept round 33): 4 pulse frames surfaced (cut at the waterline, with the foam collar), the submerged look (drawn through the generic `sub` pass, E4 = c), surfacing and diving steps, the death splash and an intel portrait (`tools/art/driftjelly.py`).

The production files (M5 part E, straight to production per E9 = a; approved as final in concept round 33), generator notes in [concept/prompts.md](concept/prompts.md):

| File | What | Status |
|---|---|---|
| [concept/driftjelly-final-r33-a.png](concept/driftjelly-final-r33-a.png) | Final sprites (`tools/art/driftjelly.py`): the plain body for the `sub` pass (`driftjelly-sub_0..3`), the surfaced dome with its foam collar (`driftjelly_0..3`), the pulse bloom, the 0.6 s surfacing steps (`driftjelly-surface_0..5`), the ripple train, the wet pop and sinking shreds (64×64), each as drawn on a sea stand-in | chosen |
| [concept/driftjelly-final-r33-a.gif](concept/driftjelly-final-r33-a.gif) | A field drifting with the sea, swapping between surfaced and submerged, ripple trains at the contractions, one shot | chosen |

## Implementation

- [x] Stat block values in a `data.yaml` loaded from data; global difficulty multipliers applied
      (M5 part E, step E2b)
- [x] Surface/submerged states: a per-unit seeded timer of 6–10 s on the real steps, a ≈ 0.6 s
      swap with the layer flip at its midpoint, a field starting about half submerged; the layer
      rule (M5 part E, step E2b)
- [x] The `field` formation: scattered units scrolling with the sea plus the current's drift
      (M5 part E, step E2b)
- [x] Proximity ring with cooldown, fired surfaced and submerged (E3 = b); hard's 12 bullets and
      110 px (M5 part E, step E2b)
- [x] Waterline foam, surfacing and diving per the water rules; the quickening pulse through the
      water (M5 part E, steps E1b and E3c: `NavalLooks`, `PulseClock`, `Level11LooksTest`)
- [x] Death effect, bounty and score per this spec; production sprites and intel portrait (M5
      part E, steps E1b and E3e; approved as final in round 33)

## Decisions

- 2026-10-01: Promoted from the naval roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: HP rescaled to the lowered L11 reference DPS (6 → 4, ×70/100).
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-08: M5 part E (user decisions of 2026-10-08): **E3 = b** surfaced and submerged jellies
  fire their ring, the pulse showing through the water as the warning; rejected: a (only surfaced
  ones fire: the fields tamer, submerged jellies only credits for torpedo owners) and c (a half ring
  with a 4 s cooldown when submerged: a second attack profile). **E2 = a**: only `anti-sub` and the
  Smart Bomb reach a submerged jelly, so `area` is struck from its traits. Stated defaults: the
  formation `field` instead of "swarm (scattered)"; scrolling with the sea when submerged; its own
  seeded swap timer (6–10 s), a field starting about half submerged, a ≈ 0.6 s swap with the layer
  flip at its midpoint, on the real steps; the veins drawn only. Our reading, for review in round 33:
  the pulse quickening from 1.5 × the trigger radius. `design` goes to `review`; `implementation`
  is `in-progress` for part E.
- 2026-10-08: M5 part E, step E2b (simulation, our readings for review in round 33): a field's
  area enters with its leading (bottom) edge at the top edge at the wave's time, its units placed
  on a seeded jittered grid at least the spacing apart (1.5 × the 28 px hit box, 42 px, when the
  wave gives none); a field unit's **first** swap comes after a seeded wait of 0–10 s (not 6–10 s),
  so the swaps show while a field crosses the screen (about 4 s) instead of all falling after it; a
  ring due while the ship is closer than enemy bullets may spawn (72 px, the bullet readability
  rule) waits until it is not, so the ring fires between 72 and 96 px; the ring's cooldown takes
  the fire-rate lever as any attack's interval, its bullets the bullet-speed lever.
- 2026-10-08: Presentation as built (M5 part E, step E3c; our readings, for review in round 33):
  the plain body (`driftjelly-sub`) in the `sub` pass always, the dome with its collar over it
  while surfaced, the swap's six steps over the resting body (forward surfacing, backward diving),
  the lime pulse's bloom (additive) over a submerged one. The pulse has its own rate, not a radial
  spinner's: one contraction a second, quickening evenly to three a second as the ship closes in
  from 180 px to the ring's reach (96 px); each jelly keeps its own phase so a change of rate never
  jumps a frame. A surfaced jelly leaves its ripple train on the sea at each contraction. A kill on
  the surface is the small water burst with its tatters, glow and ripple train; under the water the
  small under-water burst only.
- 2026-10-09: Concept round 33 closed (user): the production art (the plain `-sub` body, the surfaced dome, the ring `-pulse`, `-surface` and `-ripple`, the `small`
  death and tatters) and the intel picture approved as **final**, the weak spots as they are
  (the faint jelly ripples, the submerged jelly reading only through a quiet `sub` pass: build choice a); the stat block's numbers and the build choices of round 33 (the ring fired 72–96 px from the ship (b), each jelly's own seeded swap
  timer (c), the kill rate without torpedoes about half (w)) accepted. `design: approved`, `art: final`;
  every Implementation item is ticked, so the implementation is `done`.
