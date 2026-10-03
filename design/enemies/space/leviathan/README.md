---
title: Leviathan
design: approved
implementation: done
art: chosen
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-02
---

# Leviathan

## Summary

A huge bone-and-violet Vrell whale several times the player's length. It drifts over on `high-air` releasing Whirl Seed clusters, then descends to the play plane where its turrets, fins and blowhole can be shot. A set piece, not a boss.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis.

<!-- data: stat-block -->
| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `high-air` (first pass) → `air` (second pass) |
| Size tier | `huge` |
| Size | 300×480 px, hitbox 110×400 |
| Parts | body (`armoured`), 3 tail segments + fluke (articulated; fluke `destroyable`), 2 pectoral fins (`destroyable`), 4 dorsal turret vents (`destroyable`), blowhole (`vital`) |
| Orientation | `fixed` (per pass: the first on its diagonal course, the second facing down; tail and fluke sway) |
| HP | blowhole 150; turret vent 40 each; fin 60 each; fluke 80; total 510 (easy 382 / hard 663, from the global multipliers) |
| Armour / shield | body armoured (sparks); high-air pass: only `homing` and `beam` hit |
| Speed | 30 px/s drift; turns 10°/s |
| Movement | first pass: crosses diagonally on `high-air` in ≈ 12 s; second pass: descends (scale and shadow shrink to the air layer over 2 s) and drifts across the upper half for up to 30 s, then leaves |
| Attack | first pass: blowhole releases a whirl cluster of 6 Whirl Seeds every 3 s; second pass: each turret vent fires an `aimed` violet orb (130 px/s, `medium` = 6) every 2.2 s, staggered; fins sweep a 5-way `fan` (150 px/s) every 4 s; contact with the body `huge` = 25 |
| Formations | solo set piece |
| Weak points | blowhole (violet glow; a regular enemy, so not lime) (×2) |
| Effective traits | `piercing`, `homing`, `forward` |
| Credits | 190 (score 1900 × chain) (turret vent 20 each, fin 15 each, fluke 20, blowhole 60); killing the blowhole first destroys the rest for the full 190 |
| Death | `huge`: chained `medium` bursts along the body, ichor cloud, whale-song cry; drops a large salvage pickup |
| First level / used in | L03; Earth orbit; returns in Act 7 (L46) as a variant specified with that act |
| Difficulty hooks | hard: clusters of 8 seeds; vents fire 2-orb bursts |
<!-- /data -->

### Behaviour

- On its first pass at L03 the player has no `homing` yet: the pass is a spectacle to survive (seed clusters), not a fight.
- A set piece pays close to a mid-boss (Act 1 L03 budget ≈ 1 145, mid-boss share 15 % ≈ 170).
- It keeps one heading per pass: on the first it flies its diagonal course drawn large and
  translucent (75 % opacity) on `high-air`; on the second it faces down at the player and drifts across without turning, its
  tail and fluke swaying. Its parts are hit boxes at fixed offsets in each pass; a destroyed part
  is drawn wrecked.
- After its time on the second pass it rises back to `high-air` and leaves through the top edge,
  the way it came.

### Concept art

Chosen concept: [leviathan-r05-a.png](../concept/leviathan-r05-a.png), [leviathan-r05-a.gif](../concept/leviathan-r05-a.gif) (listed in the [space](../README.md#concept-art) Concept art table).

## Concept art

Production art for concept round 16 (M4 part C, the Level 03 batch), review files built from the final files in `assets/` by `tools/art/leviathan.py` (`--review` rebuilds only them); prompts: [concept/prompts.md](concept/prompts.md).

The ichor cloud of its death (round 16 too) is rendered by `tools/art/vrell_fx.py` (`--review ichor` rebuilds only its review files); the whale-song cry is a sound, listed in the [SFX](../../../audio/sfx/README.md#concept-art) Concept art table.

| File | What | Status |
|---|---|---|
| [concept/leviathan-final-r16-a.png](concept/leviathan-final-r16-a.png) | Final sprites: the second pass facing down (body in 3 tail-sway frames, 300×480) with its parts intact and wrecked (4 vents, 2 fins, the fluke, the additive blowhole glow), the data's hit boxes over them, and the first pass crossing diagonally at 1.25× (3 frames, heading −58.01°, along Level 03's path, which a last row draws as an arrow over frame 1); 48 colours per pass | proposed |
| [concept/leviathan-final-r16-a.gif](concept/leviathan-final-r16-a.gif) | The second pass swaying with the blowhole glow pulsing, then parts wrecked | proposed |
| [concept/leviathan-death-final-r16-a.png](concept/leviathan-death-final-r16-a.png) | Final ichor cloud (`leviathan-ichor_0..15`, 160×160, additive, 16 frames at 10 fps): a wound's violet globules and a plum-violet mist that billows and thins; one per part with its chained burst and one at the centre with the large burst | proposed |
| [concept/leviathan-death-final-r16-a.gif](concept/leviathan-death-final-r16-a.gif) | The death over the `leviathan-down` sprite: the chained medium bursts with their ichor, then the large burst | proposed |

## Implementation

- [x] High-air pass with seed clusters; descent to the air layer (simulation)
- [x] Per-part HP and destroyable parts, the armoured body glancing shots, part guns, contact
      damage once per second
- [x] Articulated animation; parts drawn wrecked; the descent and rise drawn (the tail sway in
      three frames with the fluke on its pivot; the first pass as one sprite at its heading, which
      shows no wrecked parts)
- [x] Large salvage drop
- [x] Chained death effect (a `medium` burst at each part in turn, then a large one)
- [x] Stat block values loaded from data; global difficulty multipliers applied
- [x] Bounty and score per this spec
- [x] Death effect per this spec: the ichor cloud at each part with its chained burst and at the
      centre with the large one (scaled and faded with the unit off the play plane), and the
      whale-song cry under the explosion (round 16 a, a placeholder until the recorded enemy
      sounds decide it)

## Decisions

- 2026-10-01: Promoted from the space roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-02: M4 part C (user decisions): a fixed heading per pass instead of 32 angles per part (the atlas budget: about 25 frames of a 480 px unit fit one page); the HP total is the parts' 510 (the "≈ 550" did not add up); it leaves the second pass the way it came.
- 2026-10-02: M4 part C, the simulation: the layer rules apply to both passes (only `homing` and
  `beam` reach `high-air`), so with a homing weapon fitted its parts can be hit, and wrecked, on
  the first pass too; at L03 the player owns none. Shots over the armoured body fly on to a living
  part ahead of them and glance off where none is.
- 2026-10-02 (user decision): off the play plane (the first pass on `high-air`, the second pass
  arriving, descending and rising) it is drawn at 75 % opacity, body, parts and blowhole glow alike
  (the additive glow's strength scaled with it); it is opaque on the play plane, where it collides. Descending and rising, the opacity eases with its altitude
  together with its scale, reaching full at the moment it switches below the ship, so it never pops.
- 2026-10-02: Production art, the death effect (choice for review, round 16): the ichor cloud is
  one round 160 px set played at every wound (each part with its chained burst, and the centre
  with the large burst) instead of one body-sized cloud, so it fits either pass and heading
  without turning a sprite; the whale-song cry is synthesized (`tools/concept/audio/sfx_r16.py`).
