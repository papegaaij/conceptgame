---
title: Lamprey
design: review
implementation: in-progress
art: proposed
depends-on: [../../../systems/difficulty, ../../../systems/economy, ../../../player/shields]
updated: 2026-10-12
---

# Lamprey

## Summary

A rust eel with a teal-toothed mouth disc that homes in, latches onto the Stormhawk and drains its shield until shaken off with hard left–right movement.

## Design

### Stat block

The numbers live in [data.yaml](data.yaml) (M5 part F, step 4: the `chase` movement, the `latch` block, the `drain` attack and the hooks `drain` and `latch_slots`, read by the loader). Values are balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis. Effective traits: `homing` while it chases, `side`, `rear` and `area` while it is latched.

<!-- data: stat-block -->
| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `air` |
| Size tier | `small` |
| Size | 36×36 px, hitbox 20×28 |
| Parts | single |
| Orientation | `16 angles` (16 headings × 4 swim frames; a latched pose per hull spot, a stunned pose) |
| HP | 3 (easy 2 / hard 4, from the global multipliers) |
| Armour / shield | none |
| Speed | 220 px/s (its chase at 180°/s turn rate) |
| Movement | `chase` the ship for up to 4 s at 220 px/s (turning 180°/s), then straight on; `latch` on contact at the side or tail spot it came from (1 at once; hard 2); 4 reversals of the commanded direction within 1.2 s throw it off, stunned 1 s, then it flees to the nearer side edge and never latches again; ×2 damage taken while latched |
| Attack | `drain`: 8 shield points/s while latched, on the shield only (its regeneration paused, stopping at the break; no armour damage, no speed penalty) |
| Formations | stream, swarm (3–6) (front, sides or rear; a swarm of chasers with seeded 0–0.6 s start delays) |
| Weak points | mouth disc (×1.5) (drawn only) |
| Effective traits | `homing`, `side`, `rear`, `area` |
| Credits | 12 (score 120 × chain) |
| Death | `small` wet pop |
| First level / used in | L12; Act 2 |
| Difficulty hooks | hard: drain 12/s; up to 2 latched at once (easy and medium: 1) |
<!-- /data -->

### Behaviour

The rules of M5 part F (user decisions F3 = a, F4 = a and F9 = a of 2026-10-09 and the part's
stated defaults; the schema keys in [architecture](../../../tech/architecture/README.md#data-file-schemas)):

- **Chase.** From its entry (its centre crossing the edge; in a swarm after its own seeded
  0–0.6 s delay, flying straight in until then) it steers toward the ship's centre at 220 px/s,
  turning at most 180°/s, for **4 s**; then it flies **straight on** along its heading at its speed
  and leaves the screen. The wind pushes it like the ship
  ([Level 12](../../../campaign/act-2-homefront/level-12-storm-front/README.md#weather)), in every
  state but latched.
- **Latch.** When its hit box touches the ship's while it chases or flies on and a **latch slot**
  is free (1; hard 2), it latches: it sits on the hull at the spot it came from, **facing in**:
  the **tail** when it touched from below (within 45° of straight down from the ship's centre),
  otherwise the **left or right side** by its x (a tie to the left); the spots' offsets come from
  the latched pose's art (step 2) and are data. A latched Lamprey moves with the ship. Lampreys
  latch only on the player, never on Rook ([wingmen](../../../player/wingmen/README.md#act-2-hazards)).
  With every slot taken, a touch is a `small` contact: it hurts the ship and the Lamprey dies, as
  any contact.
- **Drain.** While latched it drains **8 shield points a second** (hard 12), on the shield only,
  and the shield's **regeneration is paused** (each drain step counts as a hit for the regen
  delay). At zero shield the [shield](../../../player/shields/README.md)'s break rule applies and
  the drain stops; it never touches armour, and the shield stays down while it is latched.
  Several latched Lampreys drain each at their rate. The HUD shows `SHIELD DRAIN` and the shield
  bar flickers ([HUD](../../../ui/hud/README.md#right-panel-ship)).
- **Shake-off.** Four **reversals** of the commanded horizontal direction (`LEFT` ↔ `RIGHT`;
  neutral steps ignored, so left–neutral–left is no reversal) within **1.2 s** throw off **every**
  latched Lamprey. The rule reads the commands, not the velocity after the wind, so recorded
  inputs replay it.
- **What hits a latched Lamprey** (user decision **F3 = a**): it sits on the hull, where every shot
  spawns, so it goes by weapon, not by geometry: only shots of the **rear** mount, shots of a
  weapon with the **`side`** or **`rear`** trait, and **blasts** that reach `air` (the Proximity
  Mines', the specials') hit it, at ×2. Front and forward wing shots pass it, homing shots never
  pick it as a target, and Rook's shots pass it (his pick skips it).
- **Stun and flee** (user decision **F4 = a**: one latch per Lamprey). A shaken-off Lamprey is
  thrown outward from its spot at 120 px/s, slowing to a stop over its **1 s stun**, drifting with
  the wind, an easy kill (×1 damage). Then it **flees** at its speed to the nearer side edge and
  never latches again. Stunned or fleeing it does not collide with the ship (it was just thrown
  off the hull).
- A Lamprey drops nothing; the level's shield-cell drop factor offsets the drain.

## Concept art

Chosen concept: [lamprey-r06-a.png](../concept/lamprey-r06-a.png), [lamprey-r06-a.gif](../concept/lamprey-r06-a.gif) (listed in the [air](../README.md#concept-art) Concept art table). The production sprite (16 angles × 4 swim frames, the latched poses, the stunned pose) is made by `tools/art/lamprey.py` from the concept's model; the latch (the Lamprey on the hull with the drain flicker) is shown a/b in concept round 34 (user decision F8 = a; the game's sprite names hold look a, `--latch b` writes b). Production review files and their brief: [concept/prompts.md](concept/prompts.md). Its `data.yaml` (step 4) claims the sprites for Level 12's atlas; its 30×30 intel portrait comes from `tools/art/intel.py` (step 4, review sheet `design/ui/hangar/concept/intel-final-r34-a.png`).

| File | What | Status |
|---|---|---|
| [concept/lamprey-final-r34-a.png](concept/lamprey-final-r34-a.png) | Final sprites (`tools/art/lamprey.py`, round 34): 16 headings × 4 swim frames (36 px), the latched pose at the left, right and tail spots × 4 frames (look a), the stunned tumble (8 frames, curled, teeth dimmed), the wet pop (teal glow, additive) and the tatters (body in three, the mouth disc, fin spines; solid), on a storm-sea stand-in; one 32-colour palette over the body sets | proposed |
| [concept/lamprey-final-r34-a.gif](concept/lamprey-final-r34-a.gif) | A Lamprey chasing the ship (180°/s turn), latching on its left side, the shake-off, thrown off and stunned, fleeing; a second one shot (the wet pop with `explosion-small`); the HUD's shield row draining | proposed |
| [concept/latch-r34-a.png](concept/latch-r34-a.png) | Latch look a, "gulp": the body nearly still, swallowing in 0.4 s gulps; the ship's own shield ring pulses with each gulp (shield light drawn into the eel); at the three spots | proposed |
| [concept/latch-r34-a.gif](concept/latch-r34-a.gif) | Latch a on the Stormhawk at the left, right and tail spots, with the HUD's `SHIELD DRAIN` row flickering (8 steps on, 8 off) | proposed |
| [concept/latch-r34-b.png](concept/latch-r34-b.png) | Latch look b, "siphon": the tail lashing (the swim cycle, less), cyan shield motes spiralling into a teal glow at the mouth (`lamprey-drain`, additive, 4 frames); no shield ring | proposed |
| [concept/latch-r34-b.gif](concept/latch-r34-b.gif) | Latch b at the three spots, with the HUD's `SHIELD DRAIN` row flickering | proposed |

## Implementation

- [x] `data.yaml` with the stat block and the new keys (`chase`, `latch`, `drain`, the hooks
      `drain` and `latch_slots`), the rendered stat-block table, `first_level: 12`;
      `PartFUnitsDataTest` (M5 part F, step 4)
- [x] Chase with the turn-rate limit for 4 s, then straight on; the swarm's seeded start delays;
      wind-borne (`LampreyTest`) (M5 part F, step 3)
- [x] Latch on contact into a free slot (1, hard 2) at the side or tail spot; a `small` contact
      with every slot taken (M5 part F, step 3)
- [x] Drain on the shield only, the regen paused, stopping at the break; no speed penalty; the
      draining flag for the HUD; events `LAMPREY_LATCHED` and `LAMPREY_SHAKEN` (M5 part F, step 3)
- [x] Shake-off by four commanded reversals within 1.2 s, without allocating (a fixed ring of
      timestamps); stun with the throw and drift, then flee, never re-latching (M5 part F, step 3)
- [x] The F3 = a hit rule for a latched Lamprey (rear mount, `side`/`rear` traits, blasts; ×2);
      homing and Rook skipping it (M5 part F, step 3)
- [x] Production sprite with the latched and stunned poses, its look in the game, the wet-pop
      death, the intel portrait; in L12's atlas (M5 part F, steps 2, 5 and 6: `LampreyLooks`, drawn by
      `LevelRenderer`; `Level12LooksTest`)
- [x] Global difficulty multipliers and hooks applied; death effect, bounty and score per this
      spec; `BalanceTest`'s time to kill (M5 part F, steps 4 and 6: the multipliers, the hooks, the
      bounty and `BalanceTest`'s time to kill and latched check; the `small` wet pop with
      `lamprey-death` and `lamprey-tatters`)

## Decisions

- 2026-10-01: Promoted from the air roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: HP rescaled to the lowered L12 reference DPS (5 → 3, ×73/110).
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-09: M5 part F, **F3 = a** (user): while latched only rear-mount shots, `side`/`rear`
  weapons and blasts hit it, front and forward wing shots pass, homing and Rook skip it; rejected:
  b (by geometry, shots spawning inside its box ignoring it: fragile, the wing pods sit on the
  sides) and c (it cannot be shot while latched: against the spec's "or by shooting it").
  **F4 = a** (user): one latch per Lamprey; after the stun it flees to the nearer edge, after an
  unsuccessful chase it flies on straight; rejected: b (chasing again for the rest of its 4 s) and
  c (a fresh chase after each stun: a swarm could keep the shield at zero). **F9 = a** (user): a
  Lamprey swarm is a swarm of chasers with seeded start delays, no flocking; rejected: b (flocking
  along a route until near the ship) and c (swarms authored as tight streams).
- 2026-10-09: M5 part F stated defaults: chase 4 s then straight on; it latches only on the player;
  the latched body at the side or tail it came from, facing in; ×2 damage taken and the mouth
  disc's ×1.5 drawn only (as part C's weak points); the drain on the shield only (8/s, hard 12/s),
  the regen paused while latched, the break rule at zero, never armour; the shake-off by four
  commanded reversals within 1.2 s throwing off every latched Lamprey; with every slot taken a
  touch is a `small` contact; it drops nothing; bounty 12 in Act 1 terms, no act HP factor (new in
  L12).
- 2026-10-09: Settled in step 1 (for review in round 34): the tail spot for a touch within 45° of
  straight down, else the side by x; the throw at 120 px/s over the 1 s stun; stunned and fleeing
  Lampreys do not collide with the ship; several latched ones each drain at their rate. The
  `data.yaml` waits for step 4 (the strict loader rejects unknown keys, and every enemy's data file
  needs its portrait and sprites in the game's tests).
- 2026-10-11: M5 part F step 3 (simulation built; pending the user's review in the combined round):
  a Lamprey still flying in before its chase starts (a swarm member's delay) latches on a touch too;
  the reversals count only while something is latched (forgotten at the first latch and at each
  shake-off); the drain takes nothing in the mercy time after an armour hit (the regeneration stays
  paused) and raises no shield-hit events, only the break's; a turret does not aim at a latched
  Lamprey unless its mount may hit it; touching Rook is an ordinary contact (only a latched one
  passes him). The data's `spots` are `[dx, dy]` px from the ship's centre with y **up**, as the
  weapons' muzzles: left `[-22, -13]`, right `[21, -13]`, tail `[0, -35]` (the pivots file is in image
  coordinates, y down).
- 2026-10-11: M5 part F step 4 (pending the user's review in the combined round): the `data.yaml`
  with `movement.chase` (`speed`, `turn_rate` °/s, `seconds`), the top-level `latch` block (`slots`,
  `spots.left/right/tail` as `[dx, dy]` y up, `damage_factor`, `shake.reversals/within`, `stun`,
  `throw`), the `drain` attack (`rate`, nothing else) and the hooks `drain` and `latch_slots`;
  effective traits `homing`, `side`, `rear`, `area`; the intel portrait nose down in swim frame 1.
- 2026-10-11: M5 part F step 6, its looks in the game (pending the user's review in the combined
  round 34; latch look **a**, the "gulp", in the game, b only in the review files): chasing, flying
  on and fleeing with its 16 angles and its shadow; latched at the ship's position plus its spot,
  over the hull and without a shadow, `lamprey-latched_<spot × 4 + frame>` at 10 fps (each Lamprey
  at its own phase), the ship's shield ring pulsing 0.55 / 0.3 / 0.12 / 0 with the gulp (the
  strongest of the latched ones); thrown off, `lamprey-stunned` by the stun's progress (a full turn
  over the second) with its shadow; its death the `small` wet pop (explosion-small, `lamprey-death`
  additive, `lamprey-tatters` solid). Its sounds: the latch's bite, the drain loop while it drains,
  the shake-off (round 34 a).
