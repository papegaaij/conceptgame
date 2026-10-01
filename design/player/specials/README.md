---
title: Special abilities
design: draft
implementation: not-started
art: proposed
depends-on: [../../campaign, ../../world]
updated: 2026-10-01
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
| Airstrike | 2 charges max 4 | Two CDF bombers sweep up the screen: heavy damage (300) to `ground` and `low-air`, 60 to `air` | 300 / charge | L04 (CDF bomber support assigned after L03) | Not under ice/water; not beyond the gate | draft |
| Smart Bomb | max 3 charges | Flash: clears all enemy bullets, 120 damage to everything on screen | 400 / charge | L06 | none | draft |
| Decoy Flares | max 6 charges | Homing missiles and seekers retarget to flares for 4 s | 150 / charge | L07 | none | draft |
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

### Acts 1–2 specials in detail

First-draft balancing values. Damage of the Airstrike already includes the ×2 `anti-ground`
bonus. "Boss part" = any hittable part of a mid-boss or act boss.

#### Airstrike (L04)

| Property | Value |
|---|---|
| Charges | Bought at 300 cr each, at most 4 carried; one call uses one charge |
| Call | Press special → radio line ("Hammer flight, inbound!", text + blip). After **0.6 s** two CDF bombers enter at the bottom edge at the player's x − 64 px and x + 64 px (clamped to the play field) and fly straight up at 600 px/s (0.9 s across the screen) |
| Bombs | Each bomber drops a bomb every 36 px of travel (≈ 15 each); a bomb lands 0.25 s after release; blast radius 32 px. The strike covers a corridor about 190 px wide |
| Damage | Per blast: 100 to `ground` and `low-air` targets (hardened included), 20 to `air` targets. One strike deals at most **300** to a ground/low-air target and **60** to an air target. `high-air` and `sub` are not hit |
| Bosses | At most 150 per boss part per strike (weak-point multipliers do not apply) |
| Defence | No invulnerability; enemy bullets are not cleared |
| Repeat | A new strike can be called once the bombers have left the screen (≈ 1.5 s after the call) |
| Limits | Not under the ice or under water, not beyond the gate (see the table above) |
| Audio / VFX | `bomb` family with the full whistle plus a jet flyby (SFX list); bombers, bomb carpet and blasts on the round-08 specials sheet |

#### Smart Bomb (L06)

| Property | Value |
|---|---|
| Charges | 400 cr each, at most 3 |
| Effect | Instant. A white flash (0.1 s at 80 % opacity, fading over 0.25 s; reduced by the flash-reduction option) and a shockwave ring that expands from the ship to cover the whole play field in **0.35 s** |
| Bullets | All enemy bullets on screen are removed at once; bullets spawned while the ring expands are removed as it passes them |
| Damage | **120** once to every enemy on screen, on every layer (`air`, `low-air`, `ground` incl. hardened, `high-air`, `sub`) |
| Bosses | 60 to each boss part |
| Defence | Player invulnerable for **1.0 s** from activation (ship blinks) |
| Repeat | 1.5 s between bombs |
| Rewards | Kills pay their normal bounty and keep the chain multiplier going |
| Audio / VFX | `huge` explosion rung layered with a whoosh; flash and ring on the round-08 specials sheet |

#### Decoy Flares (L07)

| Property | Value |
|---|---|
| Charges | 150 cr each, at most 6 |
| Launch | 4 flares from the rear muzzle in a 120° fan backwards, starting at 300 px/s and slowing to a stop over 0.6 s; each burns for **4 s** |
| Effect | While any flare burns, every enemy homing projectile and seeker behaviour targets the nearest flare instead of the player, including homing shots launched during those 4 s. A homing projectile that reaches a flare detonates on it (the flare keeps burning) |
| Not affected | Aimed and straight bullets, lasers, contact attacks |
| Bosses | Same rules for boss homing attacks |
| Damage / defence | None |
| Repeat | 1.0 s between launches |
| Audio / VFX | `micromissile` family pitched up; flares on the round-08 specials sheet |

#### Common input rules

- The special button is buffered for 0.1 s. With no charge left (or in a setting where the special
  is unavailable) it plays the "special denied" sound and flashes the HUD special icon.
- Charges used in a failed attempt are restored on retry (see above).

### One special at a time

The special slot holds **one** special; there is no swap button. Specials are changed in the
hangar.

## Concept art

Concept [round 09](../../concept-rounds/round-09/README.md) — generator `tools/concept/vfx_r09.py`. Prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/specials-r09-a.png](concept/specials-r09-a.png) | Airstrike (CDF bombers, bomb carpet), Smart Bomb (flash, ring, bullets popping), Decoy Flares (homing shots retarget) (sheet) | proposed |
| [concept/specials-r09-a.gif](concept/specials-r09-a.gif) | Airstrike (CDF bombers, bomb carpet), Smart Bomb (flash, ring, bullets popping), Decoy Flares (homing shots retarget) (motion) | proposed |

## Implementation

- [ ] Special slot, charge counting and cooldown timers
- [ ] Airstrike, Smart Bomb and Decoy Flares as specified in *Acts 1–2 specials in detail*
- [ ] Setting restrictions read from the level data
- [ ] HUD icon states: ready, charges, cooldown, unavailable

## Open questions

- None open.

## Decisions

- 2026-09-30: Two special types (charge-based and cooldown-based); setting limits are a
  deliberate part of level preparation.
- 2026-09-30: Unlocks expressed as levels (`Lnn` = in the shop from the hangar visit before
  level *nn*). The Airstrike arrives at L04, matching the campaign's first-special anchor.
  Underwater limits follow [europa](../../world/europa/README.md#under-water-rules).
- 2026-10-01: One special equipped at a time (user accepted the recommendation).
- 2026-10-01: Airstrike, Smart Bomb and Decoy Flares specified in full for Acts 1–2 (timing, area, damage caps, boss rules, invulnerability, repeat delay).
