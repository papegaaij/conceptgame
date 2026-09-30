---
title: Special abilities
design: draft
implementation: not-started
art: none
depends-on: [../../campaign, ../../world]
updated: 2026-09-30
---

# Special abilities

## Summary

The special slot holds one ability, fired with its own button. **Charge-based** specials use up
charges that are bought in the hangar and occasionally found in levels. **Cooldown-based**
specials are expensive to buy but recharge on their own. Some specials depend on the setting:
no airstrikes under the Europa ice. This gives the hangar intel another thing to warn about.

## Design

| Special | Type | Effect | Cost | Unlock | Setting limits | Design |
|---|---|---|---|---|---|---|
| Airstrike | 2 charges max 4 | Two CDF bombers sweep up the screen: heavy damage (300) to `ground` and `low-air`, 60 to `air` | 300 / charge | L04 (CDF bomber support assigned after L03) | Not under ice/water; not beyond the gate | idea |
| Smart Bomb | max 3 charges | Flash: clears all enemy bullets, 120 damage to everything on screen | 400 / charge | L06 | none | idea |
| Decoy Flares | max 6 charges | Homing missiles and seekers retarget to flares for 4 s | 150 / charge | L07 | none | idea |
| EMP Burst | max 3 charges | Stuns machines 3 s and strips enemy shields; Vrell (biomechanical) stunned 1.5 s | 350 / charge | L15 | none | idea |
| Sonar Pulse | max 4 charges | Reveals the `sub` layer and makes it hittable by all weapons for 6 s | 250 / charge | L22 | Water levels only | idea |
| Orbital Lance | max 2 charges | 3 s vertical beam that follows the ship's X, 400 DPS on all layers except `sub` | 600 / charge | L17 | Needs satellite cover: not under ice, not beyond the gate | idea |
| Shield Overcharge | cooldown 45 s | Fills the shield and doubles its capacity for 8 s | 5 000 once | L19 | none | idea |
| Time Dilation | cooldown 60 s | Everything except the player runs at 50 % speed for 5 s | 8 000 once | L31 | none | idea |
| Vrell Swarm Call | cooldown 75 s | Captured Choir tech: summons 6 friendly Vrell drones for 10 s | 10 000 once | L45 | none | idea |

Rules:
- Only one special type can be equipped. Unused charges of other types stay in the inventory.
- Charges persist between levels. Charges used in a failed attempt are restored on retry.
- The special button does nothing if the special is unavailable in this setting. The HUD icon
  is greyed out and the hangar intel warns in advance.

## Implementation

- [ ] Special slot, charge counting and cooldown timers
- [ ] Each special's effect (first build: Airstrike, Smart Bomb)
- [ ] Setting restrictions read from the level data
- [ ] HUD icon states: ready, charges, cooldown, unavailable

## Open questions

- Should the special slot allow two specials (swap button), or stay at one? Recommendation:
  one, to keep the controls simple.

## Decisions

- 2026-09-30: Two special types (charge-based and cooldown-based); setting limits are a
  deliberate part of level preparation.
- 2026-09-30: Unlocks expressed as levels (`Lnn` = in the shop from the hangar visit before
  level *nn*). The Airstrike arrives at L04, matching the campaign's first-special anchor.
  Underwater limits follow [europa](../../world/europa/README.md#under-water-rules).
