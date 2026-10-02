---
title: Weapons
design: approved
implementation: done
art: chosen
depends-on: [../generator, ../../systems/economy]
updated: 2026-10-02
---

# Weapons

## Summary

Every weapon belongs to one slot type (front, rear or wing mount) and is described by the
shared **weapon traits** from the [design index](../../README.md#shared-vocabulary). Level
threat profiles recommend traits, and the hangar marks the weapons that have them. That is how
"choose the right loadout for the next level" works.

## Contents

The weapons available by L14 (Acts 1–2) have full designs; later weapons are still roster rows
below.

| Part | Summary | Design | Impl | Art |
|---|---|---|---|---|
| [pulse-cannon](pulse-cannon/README.md) | Pulse Cannon — Front, forward; DPS 20 → 70; starter; start | approved | done | final |
| [scatter-vulcan](scatter-vulcan/README.md) | Scatter Vulcan — Front, spread; DPS 18 → 65; 1 200; L02 | approved | done | final |
| [lance-laser](lance-laser/README.md) | Lance Laser — Front, piercing, forward; DPS 25 → 95; 2 500; L05 | approved | done | final |
| [hammer-mortar](hammer-mortar/README.md) | Hammer Mortar — Front, anti-ground, area; DPS 25 → 90; 1 500; L07 | approved | done | final |
| [hornet-launcher](hornet-launcher/README.md) | Hornet Launcher — Front, homing; DPS 15 → 60; 2 000; L10 | approved | not-started | chosen |
| [side-splitter](side-splitter/README.md) | Side Splitter — Rear, side; DPS 12 → 40; 1 400; L05 | approved | done | final |
| [tail-gun](tail-gun/README.md) | Tail Gun — Rear, rear; DPS 10 → 35; 600; L08 | approved | not-started | chosen |
| [fan-blaster](fan-blaster/README.md) | Fan Blaster — Rear, rear, spread; DPS 10.2 → 40; 1 200; L10 | approved | not-started | chosen |
| [proximity-mines](proximity-mines/README.md) | Proximity Mines — Rear, rear, area; DPS 20 → 70; 1 500; L12 | approved | not-started | chosen |
| [autocannon-pod](autocannon-pod/README.md) | Autocannon Pod — Wing (per pod), forward; DPS 8 → 25; 500; L02 | approved | done | final |
| [bomb-rack](bomb-rack/README.md) | Bomb Rack — Wing (per pod), anti-ground; DPS 12 → 40; 900; L03 | approved | done | final |
| [micro-missile-pod](micro-missile-pod/README.md) | Micro-missile Pod — Wing (per pod), homing; DPS 8 → 28; 800; L06 | approved | done | final |
| [swivel-gun](swivel-gun/README.md) | Swivel Gun — Wing (per pod), side, homing; DPS 8 → 26; 1 500; L09 | approved | not-started | chosen |
| [torpedo-pod](torpedo-pod/README.md) | Torpedo Pod — Wing (per pod), anti-sub; DPS 10 → 32; 1 000; L11 | approved | not-started | chosen |

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
  [layer rules](../../enemies/README.md#layer-rules). In short: all weapons hit `air`,
  `low-air` and `ground`, hardened ground targets need `anti-ground`, `high-air` takes
  only `homing` and `beam`, and the `sub` layer seen from above water takes only `anti-sub` and
  `area`. Weapons marked "ground only" (bombs, mortar shells) do not hit flying targets. Under
  water the [Europa rules](../../world/europa/README.md#under-water-rules) apply. The layer
  model itself is in [art direction](../../art-direction/README.md).
- DPS figures are first-draft balancing values; see *Data and balancing* below.

### Data and balancing

All numbers of the designed weapons (pattern, rate, damage, speed, draw, prices) live in the
`data.yaml` next to each weapon's README, the single source; the rules they share (upgrade cost
factors, draw rounding, the single-target DPS target) are in [data.yaml](data.yaml) here (schema:
[data file schemas](../../tech/architecture/README.md#data-file-schemas)). Each weapon's property
and per-level tables are rendered from it with `python3 tools/sync_tables.py`;
`python3 tools/balance.py` checks a typical player's purchases
([balance-plan.yaml](../balance-plan.yaml)) for levels 01–14 against the [credit budget](../../systems/economy/README.md#per-level-budget),
the power cap and the DPS available (and time-to-kill once enemy stat data exists, see the
script's docstring). DPS is given two ways: **volley DPS** (all shots) and **single-target DPS**
(shots that hit a 36 px target 100 px away; seeking, lobbed and dropped weapons count every
shot).

### Roster — later weapons

Still at `idea`; they get a directory when they are designed (Act 3 onwards).

| Name | Slot | Traits | DPS L1/L5 | Draw L1→L5 | Base price | Unlock | Design |
|---|---|---|---|---|---|---|---|
| Harpoon Torpedoes | front | anti-sub, forward | 30 / 100 (sub + surface) | 3 → 5 | 2 000 | L22 | idea |
| Ion Beam | front | beam | 35 / 130 (continuous) | 5 → 9 | 5 000 | L15 | idea |
| Plasma Arc | front | area, spread | 30 / 120 (short range, chains to 3–6 targets) | 5 → 8 | 6 000 | L31 | idea |
| Choir Resonator | front | spread, piercing | 45 / 160 | 7 → 11 | 12 000 | L44 (captured Vrell tech) | idea |
| Depth Charges | rear | anti-sub, rear | 25 / 80 (sink into the `sub` layer) | 2 → 3 | 1 300 | L22 | idea |
| Rear Lance | rear | rear, piercing | 18 / 60 | 3 → 5 | 2 800 | L17 | idea |
| Swarm Tail | rear | rear, homing | 15 / 55 (micro-missiles that curve back up) | 3 → 5 | 3 500 | L30 | idea |
| Deflector Pod | wing | — (defensive) | absorbs up to 3 bullets per 2 s on its side | 2 → 3 | 1 600 | L18 | idea |
| Light Drone Bay | wing | forward (drone) | 10 / 30. See [wingmen](../wingmen/README.md) | 2 → 3 | 2 000 | L15 | idea |
| Tesla Coil Pod | wing | area, shield-breaker | 12 / 40 (short-range zap) | 2 → 4 | 3 000 | L29 | idea |

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

## Concept art

Concept [round 08](../../concept-rounds/round-08/README.md) — player projectile families; generator `tools/concept/vfx_r08.py`. Prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/projectiles-r08-a.png](concept/projectiles-r08-a.png) | All 13 projectile families: sprite, 3-frame muzzle flash, 4-frame impact, L1/L3/L5 patterns, missile plumes and trails (sheet) | chosen — beam needs an impact effect where it hits (round 09) |
| [concept/projectiles-r08-a.gif](concept/projectiles-r08-a.gif) | The families firing in sequence from the Stormhawk with fitted pods (motion) | chosen — beam needs an impact effect where it hits (round 09) |

Concept [round 09](../../concept-rounds/round-09/README.md) — generator `tools/concept/vfx_r09.py`.

| File | What | Status |
|---|---|---|
| [concept/beam-impact-r09-a.png](concept/beam-impact-r09-a.png) | Beam impact: the beam bores into the silhouette and ends in a contact flare with back-sparks, scorch and heat shimmer; off-screen when nothing is hit (sheet) | chosen |
| [concept/beam-impact-r09-a.gif](concept/beam-impact-r09-a.gif) | Beam impact: the beam bores into the silhouette and ends in a contact flare with back-sparks, scorch and heat shimmer; off-screen when nothing is hit (motion) | chosen |

Concept [round 14](../../concept-rounds/round-14/README.md) — the Act 1 arsenal's effects as final art; generator `tools/art/weapon_fx.py`.

| File | What | Status |
|---|---|---|
| [concept/weapons-final-r14-a.png](concept/weapons-final-r14-a.png) | Final effects: the Scatter Vulcan, Autocannon and Side Splitter shots at every angle their patterns use, the lance per level, the micro-missile at 32 headings, the bomb and the shell, the ballistic and launcher muzzle flashes, the ballistic and explosive impacts (sheet) | chosen |
| [concept/weapons-final-r14-a.gif](concept/weapons-final-r14-a.gif) | The Stormhawk firing the fan, the pods and the side guns, a missile turning through its headings (motion) | chosen |
| [concept/weapons-capture-final-r14-a.png](concept/weapons-capture-final-r14-a.png) | Game captures of Level 01 with `--loadout`: Vulcan, Side Splitter and bombs; the Lance and two missile pods; the Mortar, bombs and the Autocannon | chosen |

## Implementation

- [x] Weapon data format: slot, traits, per-level damage/pattern/draw, price, unlock (the
  fields of each weapon's `data.yaml`)
- [x] Projectile patterns with L1–L5 (+ overdrive) variants for the Act 1 arsenal and the Pulse Cannon
- [ ] The Act 2 weapons (Tail Gun, Fan Blaster, Proximity Mines, Hornet Launcher, Swivel Gun, Torpedo Pod) — **later: M5** (their levels)
- [x] Layer hit rules per trait for the Act 1 arsenal: `anti-ground` (hardened targets, ×2 on the ground for bolts), homing reaching `high-air`, ground-only blasts (`area` of the mortar)
- [ ] `anti-sub` and the mines' `area` — **later: M5**; `beam` — **later: Act 3** (the Ion Beam, L15)
- [x] Hangar trait markers linked to the level threat profile

## Open questions

- Should the Ion Beam keep `shield-breaker` after all? That would make shield-breaking available
  from L15 instead of L29 and weaken the Act 5 shop moment.
- The roster is large (24 entries). For the first playable build (Acts 1–2) the 14 designed
  weapons are needed; the earlier suggestion to start with Pulse Cannon, Scatter Vulcan, Tail
  Gun, Micro-missile Pod and Bomb Rack still holds as the order of implementation.

## Decisions

- 2026-09-30: Weapons are described by the shared trait vocabulary; every weapon has L1–L5.
- 2026-09-30: Rows stay in the roster until a weapon gets a detailed design (per CLAUDE.md).
- 2026-09-30: Unlocks expressed as levels and aligned with the campaign's trait pacing: Bomb
  Rack to L03, Lance Laser and Side Splitter to L05, Hammer Mortar to L07, Tail Gun to L08,
  Torpedo Pod to L11 (optional early anti-sub). The Ion Beam loses `shield-breaker` (it is the
  `beam` answer from L15); the Tesla Coil Pod brings `shield-breaker` at L29, as the campaign
  paces it.
- 2026-09-30: Layer hit rules are owned by [enemies](../../enemies/README.md#layer-rules).
- 2026-10-01: Layer hit rules settled (see [enemies](../../enemies/README.md#layer-rules)): `anti-ground` does not add hits on `low-air` (every weapon already hits it); hardened ground targets need `anti-ground`.
- 2026-10-01: The 14 weapons available by L14 promoted to their own documents with per-level numbers in `balance-data.json` (single source, tables generated by `tools/balance.py --sync`). Spread fans (Scatter Vulcan L4–L5, Fan Blaster L4–L5) get a dense core so single-target damage rises with every level.
- 2026-10-01: Concept round 08: projectile families chosen ("very nice"); the beam needs an impact effect where it hits — it currently ends abruptly at the sprite edge (round 09).
- 2026-10-01: Concept round 09: beam impact chosen.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../reviews/acts-1-2/README.md).
- 2026-10-02: Weapon numbers moved from `balance-data.json` into a `data.yaml` per weapon plus this directory's [data.yaml](data.yaml) for the shared rules (M2 data files); `tools/sync_tables.py` renders each weapon's property and per-level tables, `tools/balance.py --sync` is gone.
- 2026-10-02: M3 part B2: every designed weapon is sold in the hangar from its unlock level with
  its price, upgrade costs and draw; ◆ markers count a weapon's traits that the next level
  recommends, shown from sensor L3 on (the level at which the intel reveals the recommendation).
  Only the Pulse Cannon flies so far; the others fly from M4.
- 2026-10-02: M4 part A built (user decisions at the part's start: a `--loadout` debug option to fly them on Level 01; the micro-missile's `range` is its seek radius and it lives 1.2 s; missiles seek enemies only and the mortar snaps to destructible ground targets, never a secret's beacon; the effects come from a production generator, reviewed in round 14). The simulation flies every weapon whose delivery it knows (bolts, homing, dropped, lobbed): the Act 1 arsenal, and the Act 2 Tail Gun, Fan Blaster, Hornet Launcher and Swivel Gun with borrowed effects until their levels; the mines and the Torpedo Pod do not fly yet.
- 2026-10-02: Concept round 14 closed (user decision): the Act 1 arsenal's effects approved as **final**; the seven weapons have `art: final`. This doc's `art` stays `chosen` while the Act 2 weapons have concept art only.
