---
title: Spore Bomber
design: approved
implementation: done
art: final
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-03
---

# Spore Bomber

## Summary

A slow olive gas-bag on the `low-air` layer that drops drifting spore mines; the mines rise to the player's plane after a second. Spread fire clears them.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis.

<!-- data: stat-block -->
| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `low-air` (bomber); its spore mines rise to `air` |
| Size tier | `medium` |
| Size | 72×72 px, hitbox 52×56 |
| Parts | single |
| Orientation | `fixed` |
| HP | 24 (easy 18 / hard 31, from the global multipliers) |
| Armour / shield | none |
| Speed | 45 px/s |
| Movement | `straight` down the screen or `strafe` across at y = 100–300 px |
| Attack | `mine`: drops a spore every 1.6 s; spores drift 20 px/s in a random direction, rise to the player plane after 1 s (lime glow brightens), burst on contact (damage `medium` = 6) or after 8 s into a 6-bullet `ring` (90 px/s, damage `small` = 4). Spores are shootable, 1 HP |
| Formations | line abreast (3), convoy |
| Weak points | lime spore bulbs (drawn only) |
| Effective traits | `spread`, `area` |
| Credits | 25 (score 250 × chain); spores 1 each |
| Death | `medium` burst: olive membrane tatters, lime spore cloud (harmless) |
| First level / used in | L03; Act 1 and Act 2 area denial |
| Difficulty hooks | hard: spores burst into 8-bullet rings; easy: spores never burst on their own |
<!-- /data -->

### Behaviour

- Being on `low-air`, the bomber does not collide with the player and is hit by all weapons.
- Spores on the low layer (first second) are not yet dangerous and cannot be hit; this teaches the layer model.

### Concept art

Chosen concept: [spore-bomber-r04-a.png](../concept/spore-bomber-r04-a.png) (listed in the [air](../README.md#concept-art) Concept art table).

## Concept art

Production art for concept round 16 (M4 part C, the Level 03 batch), review files built from the final files in `assets/` by `tools/art/vrell_l03.py` (`--review` rebuilds only them); prompts: [concept/prompts.md](concept/prompts.md).

The death effect beyond the `medium` burst (round 16 too) is rendered by `tools/art/vrell_fx.py` (`--review bomber` rebuilds only its review files).

| File | What | Status |
|---|---|---|
| [concept/spore-bomber-final-r16-a.png](concept/spore-bomber-final-r16-a.png) | Final sprites: the 4-frame idle loop (72×72, 32 colours, facing down), the spore mine's 4-frame additive pulse (14×14) and the mine as drawn while it rises | chosen |
| [concept/spore-bomber-final-r16-a.gif](concept/spore-bomber-final-r16-a.gif) | A bomber flying down the screen, dropping drifting spores | chosen |
| [concept/spore-bomber-death-final-r16-a.png](concept/spore-bomber-death-final-r16-a.png) | Final death effect: the lime spore cloud (`spore-bomber-death_0..15`, 96×96, additive) and the olive membrane tatters with chitin bits (`spore-bomber-tatters_0..15`, 96×96, solid, ray-marched from the bomber's materials), 16 frames at 15 fps each, shown alone and with the medium burst | chosen |
| [concept/spore-bomber-death-final-r16-a.gif](concept/spore-bomber-death-final-r16-a.gif) | A bomber flying down the screen and dying: medium burst, tatters and spore cloud | chosen |

## Implementation

- [x] No collision on `low-air` (simulation)
- [x] Low-air rendering (at its sprite size above the ground, below the low-air banks)
- [x] Spore mine lifecycle: drop, rise after 1 s, contact burst, timed ring (simulation; the
      ring keeps the bullet rule of no bullet within 72 px of the ship)
- [x] Spore mines drawn (rising glow, drawn above the haze like bullets)
- [x] Spores shootable for 1 credit
- [x] Stat block values loaded from data; global difficulty multipliers applied
- [x] Bounty and score per this spec
- [x] Death effect per this spec: the `medium` burst
- [x] Death effect per this spec: the olive membrane tatters (solid, under the glows) and the
      lime spore cloud (additive), both with the `medium` burst

## Decisions

- 2026-10-01: Promoted from the air roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-02: Production art, the death effect (choice for review, round 16): two sets played
  with the `medium` burst, the spore cloud additive (`spore-bomber-death`) and the membrane
  tatters solid (`spore-bomber-tatters`, drawn under the glows): opaque olive hide added as light
  would glow instead of reading as torn skin.
- 2026-10-03: Concept round 16 closed (user decision): the production sprites, the spore mine and the death effect (spore cloud and membrane tatters) approved as **final**, `art: final`.
