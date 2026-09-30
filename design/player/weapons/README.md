---
title: Weapons
design: draft
implementation: not-started
art: none
depends-on: [../generator, ../../systems/economy]
updated: 2026-09-30
---

# Weapons

## Summary

Every weapon belongs to one slot type (front, rear or wing mount) and is described by the
shared **weapon traits** from the [design index](../../README.md#shared-vocabulary). Level
threat profiles recommend traits, and the hangar marks the weapons that have them. That is how
"choose the right loadout for the next level" works.

## Design

### Common rules

- **Upgrade levels 1–5.** Each level raises damage and/or adds projectiles and raises the power
  draw. Overdrive pickups temporarily add a 6th level pattern.
- **Upgrade cost** (first draft): L2 = 0.5 × base price, L3 = 1 ×, L4 = 2 ×, L5 = 4 ×. Taking a
  weapon from L1 to L5 costs 7.5 × its base price on top of the purchase.
- **Draw**: listed as L1 → L5 in MW. In between it rises linearly and is rounded to 0.5.
- **Unlock**: `Lnn` = the item is in the shop from the hangar visit before level *nn*. The
  trait pacing (which trait must be buyable by which level) is owned by the
  [campaign](../../campaign/README.md#loadout-pressure-and-shop-unlock-pacing); the unlock
  levels here are set to match it. Some items are unlocked by data cores or story events
  (captured tech).
- **Layers**: which weapons hit which layer is defined once in the enemy
  [layer rules](../../enemies/README.md#layer-rules-proposal). In short: all weapons hit `air`,
  `low-air` and `ground`, hardened ground targets need `anti-ground` or `area`, `high-air` takes
  only `homing` and `beam`, and the `sub` layer seen from above water takes only `anti-sub` and
  `area`. Weapons marked "ground only" (bombs, mortar shells) do not hit flying targets. Under
  water the [Europa rules](../../world/europa/README.md#under-water-rules) apply. The layer
  model itself is in [art direction](../../art-direction/README.md).
- DPS figures are for L1 / L5 against a single target and are first-draft balancing values.

### Roster — front guns

| Name | Traits | DPS L1/L5 | Draw L1→L5 | Base price | Unlock | Design |
|---|---|---|---|---|---|---|
| Pulse Cannon | forward | 20 / 70 | 2 → 4 | starter (upgrades priced as 600) | start | idea |
| Scatter Vulcan | spread | 18 / 65 (3-way → 7-way fan) | 3 → 5 | 1 200 | L02 | idea |
| Hornet Launcher | homing | 15 / 60 | 3 → 6 | 2 000 | L10 | idea |
| Hammer Mortar | anti-ground, area | 25 / 90 (ground only) | 3 → 5 | 1 500 | L07 | idea |
| Lance Laser | piercing, forward | 25 / 95 | 4 → 7 | 2 500 | L05 | idea |
| Harpoon Torpedoes | anti-sub, forward | 30 / 100 (sub + surface) | 3 → 5 | 2 000 | L22 | idea |
| Ion Beam | beam | 35 / 130 (continuous) | 5 → 9 | 5 000 | L15 | idea |
| Plasma Arc | area, spread | 30 / 120 (short range, chains to 3–6 targets) | 5 → 8 | 6 000 | L31 | idea |
| Choir Resonator | spread, piercing | 45 / 160 | 7 → 11 | 12 000 | L44 (captured Vrell tech) | idea |

### Roster — rear guns

| Name | Traits | DPS L1/L5 | Draw L1→L5 | Base price | Unlock | Design |
|---|---|---|---|---|---|---|
| Tail Gun | rear | 10 / 35 | 1 → 3 | 600 | L08 | idea |
| Fan Blaster | rear, spread | 10 / 40 (3 → 5 shots angled back) | 2 → 4 | 1 200 | L10 | idea |
| Side Splitter | side | 12 / 40 (fires left and right, 90°) | 2 → 4 | 1 400 | L05 | idea |
| Proximity Mines | rear, area | 20 / 70 (dropped, 4 s life) | 2 → 4 | 1 500 | L12 | idea |
| Depth Charges | anti-sub, rear | 25 / 80 (sink into the `sub` layer) | 2 → 3 | 1 300 | L22 | idea |
| Rear Lance | rear, piercing | 18 / 60 | 3 → 5 | 2 800 | L17 | idea |
| Swarm Tail | rear, homing | 15 / 55 (micro-missiles that curve back up) | 3 → 5 | 3 500 | L30 | idea |

### Roster — wing mounts

Prices and draw are per pod; most players fit a pair.

| Name | Traits | DPS L1/L5 | Draw L1→L5 | Base price | Unlock | Design |
|---|---|---|---|---|---|---|
| Autocannon Pod | forward | 8 / 25 | 1 → 2 | 500 | L02 | idea |
| Micro-missile Pod | homing | 8 / 28 | 1 → 3 | 800 | L06 | idea |
| Bomb Rack | anti-ground | 12 / 40 (ground only) | 1 → 2 | 900 | L03 | idea |
| Swivel Gun | side, homing | 8 / 26 (auto-aims at the nearest enemy, 360°) | 2 → 3 | 1 500 | L09 | idea |
| Torpedo Pod | anti-sub | 10 / 32 | 1 → 3 | 1 000 | L11 | idea |
| Deflector Pod | — (defensive) | absorbs up to 3 bullets per 2 s on its side | 2 → 3 | 1 600 | L18 | idea |
| Light Drone Bay | forward (drone) | 10 / 30. See [wingmen](../wingmen/README.md) | 2 → 3 | 2 000 | L15 | idea |
| Tesla Coil Pod | area, shield-breaker | 12 / 40 (short-range zap) | 2 → 4 | 3 000 | L29 | idea |

### Coverage check

Every trait is buyable from the level the campaign's pacing table requires. First item per
trait (the campaign table is the reference for the levels):

| Trait | Required from | First item(s) |
|---|---|---|
| forward | start | Pulse Cannon (starter), Autocannon Pod L02 |
| spread | L02 | Scatter Vulcan L02 |
| anti-ground | L03 | Bomb Rack L03; Airstrike special L04; Hammer Mortar L07 |
| piercing | L05 | Lance Laser L05 |
| side | L05 | Side Splitter L05; Swivel Gun L09 |
| homing | L06 | Micro-missile Pod L06; Swivel Gun L09; Hornet Launcher L10 |
| area | L07 | Hammer Mortar L07; Proximity Mines L12 |
| rear | L08 | Tail Gun L08; Fan Blaster L10 |
| anti-sub | L11 | Torpedo Pod L11; Harpoon Torpedoes, Depth Charges and the Sonar Pulse special L22 |
| beam | L15 | Ion Beam L15 |
| shield-breaker | L29 | Tesla Coil Pod L29 (the EMP Burst special strips shields from L15) |

## Implementation

- [ ] Weapon data format: slot, traits, per-level damage/pattern/draw, price, unlock
- [ ] Projectile patterns for every roster entry with L1–L5 (+ overdrive) variants
- [ ] Layer hit rules per trait (anti-ground, anti-sub, beam, area)
- [ ] Hangar trait markers linked to the level threat profile

## Open questions

- Should `anti-ground` shots also hit `low-air`? Recommendation: no, so the choice stays clear.
- Should the Ion Beam keep `shield-breaker` after all? That would make shield-breaking available
  from L15 instead of L29 and weaken the Act 5 shop moment.
- The roster is large (24 entries). For the first playable build, implement Pulse Cannon,
  Scatter Vulcan, Tail Gun, Micro-missile Pod and Bomb Rack.

## Decisions

- 2026-09-30: Weapons are described by the shared trait vocabulary; every weapon has L1–L5.
- 2026-09-30: Rows stay in the roster until a weapon gets a detailed design (per CLAUDE.md).
- 2026-09-30: Unlocks expressed as levels and aligned with the campaign's trait pacing: Bomb
  Rack to L03, Lance Laser and Side Splitter to L05, Hammer Mortar to L07, Tail Gun to L08,
  Torpedo Pod to L11 (optional early anti-sub). The Ion Beam loses `shield-breaker` (it is the
  `beam` answer from L15); the Tesla Coil Pod brings `shield-breaker` at L29, as the campaign
  paces it.
- 2026-09-30: Layer hit rules are owned by [enemies](../../enemies/README.md#layer-rules-proposal).
