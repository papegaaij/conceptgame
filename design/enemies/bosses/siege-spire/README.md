---
title: Siege Spire
design: review
implementation: in-progress
art: chosen
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-12
---

# Siege Spire

## Summary

The Act 2 boss: a slate citadel rooted in Geneva Concord under the Vrell canopy. Root turrets and mortars protect a trunk whose maw launches Wraiths; when the roots die, the spire tears itself free and rises for a final airborne phase.

**Status (M5 part H, step H4).** The stat block and the boss script are in [data.yaml](data.yaml) and loaded (step H4); the fight is measured and the HP tuned (H10 = a). **The user is on vacation: every decision is the recommendation and pending the user's review in the combined round 36 at the end of M5; H1 drops the approved 15 px/s creep, a change to an approved spec that the user must confirm.**

## Design

### Stat block

Values are balancing numbers at **medium**, in [data.yaml](data.yaml) with the boss script (`boss`: the phases, the free pose, the maw's windows and their ambush launches, the lift, the par; the anchored arrival and the free spire's sway; M5 part H, step H4, keys in the [schemas](../../../tech/architecture/README.md#data-file-schemas)) (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis.

<!-- data: stat-block -->
| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `ground` (the roots, the trunk and the maw) → `air` (the free spire after the lift, phase 3: a ship touching it takes the `huge` contact damage) |
| Size tier | `huge` |
| Size | 440×440 px, hitbox 380×380 (the rooted body: the trunk ≈ 180 × 240 px with its lean, the roots ≈ 380 px across); the free spire 160×200 hitbox, drawn 244 px |
| Parts | 4 root turret pods (`destroyable`, `ground`), 2 acid-mortar roots (`destroyable`, `ground`), the launch maw (`destroyable`, ×2, exposed in phase 2 only), the spire core (`vital`, ×2, exposed in phase 3, `air` after the lift), the trunk (`armoured`: shots glance, no HP, not in the bar; it stays at the anchor as the stump) |
| Orientation | `32 angles` (the turret pods; the free spire 16 pre-rendered headings over its six-fold symmetry) |
| HP | turret pod 665 each (2,660); mortar root 775 each (1,550); maw 2,880 (×2); core 4,950 (×2); the spec's ×1.8, tuned to the measured fight (H10 = a); total 12040 (easy 9030 / hard 15652, from the global multipliers) |
| Armour / shield | trunk armoured (shots glance off it; it stays as the stump); the roots and the maw are ground parts (all weapons, `anti-ground` ×2); the maw ×2 and the core ×2; invulnerable during the lift |
| Speed | 0 px/s (anchored); the free spire sways ±100 px, 50 px/s at most |
| Movement | anchored at the Concord rotunda: it scrolls in with the ground (invulnerable, without its bar) and engages when the scroll halts, the anchor at y 196; phase 3 lifts it off the anchor in 3 s (invulnerable, eased) to x 240, y 120, where it sways; the stump stays (see *Phases*) |
| Attack | see *Phases*: phase 1: each turret pod fires an `aimed` violet orb (150 px/s, `medium` = 6) every 1.6 s, the four staggered; the two mortar roots `lob` acid every 4 s each, staggered: a lime marker on the ship's position 1 s ahead, a ship inside the 40 px impact circle takes `heavy` = 10, then a ring of 10 bullets (110 px/s, `small` = 4); phase 2: the maw spits a 9-way `fan` (spread 80°, 140 px/s, `medium` = 6) every 2.5 s; phase 3: the free spire's `laser-sweep` from the core, a beam 300 px long sweeping a 120° arc over 1.2 s, centred on the ship's bearing when the 0.6 s telegraph starts (the six crimson emitters charge), `laser` = 8 once per sweep, every 5 s; between the sweeps a 14-bullet `ring` (120 px/s, `small` = 4) |
| Formations | solo set piece (the maw launches Wraiths in phase 2) |
| Weak points | lime maw (×2), lime spire core (×2) (the maw in phase 2, the core in phase 3) |
| Effective traits | `anti-ground`, `rear`, `piercing` |
| Credits | 372 (score 3720 × chain) (pods and mortar roots 31 each, maw 62, core 124: the spec's ratios; × 1.6 × Level 14's `bounty_scale` 0.76 it pays 452, 26.8 % of budget(14) = 1,687) |
| Death | `huge`: the spire cracks open, chained explosions over 3 s, the canopy above withers and light breaks through (the `boss-destroyed` event starts the level's daylight); credit shower |
| First level / used in | L14; L14 only (an echo version in the L49 boss rush) |
| Difficulty hooks | hard: the lob's ring of 12, the sweep 180°, the maw launches 3 Wraiths |
<!-- /data -->

**The measured fight (H10 = a, `SiegeSpireTest`).** The autopilot with the balance plan's Level 14 fit (the front at L4, two Micro-missile Pods at L2, the Tail Gun at L2, Armour Mk III, Rook's Autocannon) on a fixture arena, five seeds: with the spec's HP (6,690) the fight lasted ≈ 76 s at medium (the roots ≈ 42 s, the maw ≈ 12 s, the free spire ≈ 21 s): the plan's DPS on the roots (`anti-ground` from Rook) and on the ×2 maw and core is well above the paper's 0.6 × 80. Every HP was scaled ×1.8 (pod 370 → 665, mortar root 430 → 775, maw 1,600 → 2,880, core 2,750 → 4,950; total 12,040): **medium 126.1–131.7 s, mean 129.5 s** (the roots ≈ 72 s, the maw ≈ 21 s with three launches of two Wraiths, the lift and the free spire ≈ 36 s; remeasured in step H6 after pod 1's offset was fixed: it was 132.4 s with the pod off its root), every seed inside the act-boss window of 90–180 s; **the par is 130 s**. Easy (×0.75 HP): 93–98 s, every seed won. Hard (×1.3 HP, the ring of 12, the 180° sweep, 3 Wraiths a launch): 190–205 s on the four seeds won, one seed lost (the autopilot is worn down in the roots phase); the hard fight is longer than the window because the autopilot lines up over every launched Wraith. The paper estimate at 0.6 × the reference DPS (the ease, the lift, the delays and the parts' HP at their multipliers, `BalanceTest`, `tools/balance.py`): 174 s, inside the window. In Level 14 itself (seed 2185) the fight lasted 97 s on easy, 136 s on medium and 204 s on hard, all won on the first attempt.

### Phases

The boss is one anchored arena boss with one HP bar and no slam lanes (the model is the [Harbour Kraken](../harbour-kraken/README.md)): it is anchored at the Concord rotunda, the level's arena halts the scroll at t=108 and **all six roots are on the screen from the start**. The part offsets from the anchor follow the chosen concept: the trunk at the top of the field, the four turret pods and the two mortar roots spread along the avenues. **The 15 px/s creep of phase 1 is dropped** (H1 = a, pending the user's review): "in turn" comes from the staggered fire and from the player choosing which root to kill first, since the roots' guns turn toward the ship.

| Phase | Ends at | Behaviour |
|---|---|---|
| 1 — Roots | roots 4 210 → 0 | Turret pods fire `aimed` violet orbs (150 px/s, `medium` = 6) every 1.6 s each, staggered; mortar roots **`lob`** acid every 4 s, staggered between the two (marker 1.0 s on the ship's position at the throw, then the flight and a 10-bullet ring at the impact; hard: a ring of 12). The roots are `ground` parts: all weapons hit them, `anti-ground` ×2. |
| 2 — Maw | maw 2 880 → 0 | The claw crown opens (the maw is exposed from the phase's start, a lime ×2 weak spot) and the maw launches **2 Wraiths every 8 s** (hard 3) and spits a 9-bullet `fan` (spread 80°, 140 px/s) every 2.5 s. The launched Wraiths are ambushes (H3 = a, below). Destroyed roots stay dead. The maw's death drops an overdrive. |
| 3 — Free spire | core 4 950 → 0 | A **lift** (H4 = a): the whole boss lifts off its anchor to a station in the upper half over ≈ 3 s (invulnerable, as moves are), its core turning to `air` (a ship touching it takes contact damage, `huge` 25) and the pose to the free spire; the tear-free shockwave is an event (a ring of dust and debris drawn by the game, no collision) and drops one armour patch at the start. The stump and the debris stay at the anchor as scenery (drawn from the boss's state, not as a backdrop piece). The spire then drifts across the upper half at 50 px/s (sway ±100 px) firing a telegraphed **`sweep`** (below) every 5 s and `ring`s of 14 in between; the core is a lime ×2 weak spot. |

#### Attacks added for this boss (H5 = a)

- **`lob`** (a part's attack): built on the Polyp Mortar's lob and generalised to a part as the origin. The marker (1.0 s) is placed on the ship's position at the throw; the flight; the impact ring (10 bullets, hard 12); events for the sound; interval 4 s per mortar root, staggered; its state is hashed only when a level has it.
- **`sweep`** (the airborne spire's `laser-sweep`): built on the Mantis's sweep, generalised from the enemy to a part as the origin (the spire's core); a 120° arc (hard 180°), a 0.6 s telegraph line, then the sweeping beam: `laser` = 8 per touch, once per sweep, against the ship and Rook; every 5 s, interleaved with the `ring`s of 14. The airborne spire keeps the lower half free so that the sweep and the rings stay dodgeable.

#### The maw's Wraiths (H3 = a)

A launched [Wraith](../../air/wraith/README.md) starts **cloaked** at the maw's centre, swoops down a lane toward the player's side past the ship, re-enters from the **bottom edge** after the 1.5 s gap, decloaks, holds 2.5 s and exits up a side lane, as in Level 10. For each re-entry the sim adds the **3 s bottom-edge warning** (computed from the launch tick, deterministic) and counts the bottom edge as a rear wave for Rook (Trail formation, his rear bark). **At most 4 Wraiths from the maw are alive at once.** They are counted as the level's enemies and pay the normal Wraith bounty (see below).

### Arena

Geneva Concord under the Vrell canopy ([Level 14](../../../campaign/act-2-homefront/level-14-siege-spire/README.md)). The length is **measured** (H10 = a, see *Stat block*): the autopilot's fights on the balance plan's fit over five seeds, the HP tuned (not the attacks, ×1.8 on every part) to a mean of 129.5 s at medium (remeasured in step H6), every seed in the act-boss window of 90–180 s; the par is the measured mean, 130 s. The boss bar `SIEGE SPIRE` (the long bar) appears at the arena start, t=108, together with the warning, the sting and the checkpoint (none on hard). The arena section runs 108–250 s (the level clock halts at its end while the Spire lives, a kill jumps it there); the level continues with a 12 s daylight section after the kill.

### Behaviour

- The boss teaches the Act 2 lessons in order: ground targets (roots), rear awareness (Wraiths), then dodging in the open.
- Wraiths launched here use their own spec (rust with blue-violet veins) and pay the normal
  [Wraith](../../air/wraith/README.md) bounty (30 in Act 1 terms, paid × 1.6 × the level's `bounty_scale`) on top of the boss bounty; levels budget them at the expected count (4 launches of 2; see
  [economy](../../../systems/economy/README.md#sources)).

## Concept art

Chosen concept: [siege-spire-r06-a.png](../concept/siege-spire-r06-a.png), [siege-spire-r06-a.gif](../concept/siege-spire-r06-a.gif) (listed in the [bosses](../README.md#concept-art) Concept art table). It goes straight to production (H8 = a): the rooted body, the pods (32 headings), the mortar roots (shut and lob), the maw, the crown, the tear-free sequence, the free spire, the stump and the death chain. Generator notes: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/siege-spire-final-r36-a.png](concept/siege-spire-final-r36-a.png), [.gif](concept/siege-spire-final-r36-a.gif) | Production art (`tools/art/siege_spire.py`, in `assets/` since step H4): trunk (3 maw steps), six roots alive/dead, pods (32 headings), mortars, husks, the stump, the lift, the free spire (16 headings over 60°), emitter and maw glows, debris; assembled phases 1–3 and the fight GIF; about 41 % of one atlas page | proposed |
| [concept/siege-spire-death-final-r36-a.png](concept/siege-spire-death-final-r36-a.png), [.gif](concept/siege-spire-death-final-r36-a.gif) | Production art (`tools/art/siege_spire_death.py`, `assets/sprites/siege-spire-death_0..7`, step H5; notes in [concept/prompts.md](concept/prompts.md#siege-spire-death-final-r36-a)): the death in the free pose over the 3 s chain, the core's lime light cracking out along the six petals (0–3), then the six petal chunks flying apart and burning down to embers (4–7); the game's chained explosions go over it | proposed |

## Implementation

- [x] Spec reworked to the decided design: no creep, one anchored boss without lanes, the lift, `lob` and `sweep`, the maw's ambush launches, the bounty as a share of the budget (M5 part H, step H1a)
- [x] Rooted phase with six independent root parts on `ground`, all on the screen from the arena start, no creep (M5 part H, step H3)
- [x] `lob` from the mortar roots: marker, flight, ring, staggered, hard's ring of 12 (M5 part H, step H3)
- [x] The maw (exposed in phase 2), its 9-bullet fan, and the Wraith launches as ambush plans with the dynamic bottom-edge warning, Rook's Trail and at most 4 alive (M5 part H, step H3)
- [x] Tear-free lift: invulnerable ≈ 3 s, core to `air`, shockwave event, armour patch, the stump; the airborne drift, `ring`s of 14 and the `sweep` (hard 180°) (M5 part H, step H3)
- [x] `data.yaml` with the stat block loaded and the global difficulty multipliers applied (M5 part H, step H4)
- [x] The fight measured over five seeds (mean 129.5 s, every seed 90–180 s), the HP tuned, the par (130 s) and the bounty (452 as paid, 26.8 %) set; `SiegeSpireTest` (M5 part H, step H4)
- [x] Death effect, bounty and score per this spec; the canopy's death handing over to the daylight (M5 part H, steps H4 and H6: the death frames over the 3 s chain with the chained bursts, the screen flash and the credit shower; `SpireLooks`, `Level14LooksTest`, `LevelScreenPartHTest`)
- [x] The presentation in the game (M5 part H, step H6, `SpireLooks`): the roots alive and withered, the trunk with the crown opening as the maw is exposed, the turret pods turned toward the ship, the mortar pods opening for their lobs, the husks, the hit flashes, the maw's bloom at each launch, the lift frames gliding from the trunk to the station over the stump, the tear-free shockwave (a dust ring and shards), the free spire turning, its emitters charging through the sweep's telegraph, the telegraph line and the beam, its shadow
- [ ] Production art: sprites, the death chain, the stump, the shockwave debris, the intel silhouette (M5 part H, step H5)

## Decisions

- 2026-10-01: Promoted from the bosses roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: HP rescaled to the lowered L14 reference DPS (×80/130): turret pods 600 → 370, mortar roots 700 → 430, maw 2 600 → 1 600, spire core 4 500 → 2 750, total 10 900 → 6 690; duration ≈ 139 s (was ≈ 140 s). Bounty unchanged.
- 2026-10-01: Maw-launched Wraiths pay the normal Wraith bounty at the Act 2 factor, budgeted at the expected count as in L14 (user decision).
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-11: M5 part H, step H1a (the user is on vacation: H1–H10 are all the gap analysis's recommendation, pending the user's review in the combined round 36 at the end of M5; the full list with the rejected variants is in the [Level 14 decisions](../../../campaign/act-2-homefront/level-14-siege-spire/README.md#decisions)). **H1 = a (recommended; pending the user's review): the approved 15 px/s creep of phase 1 is dropped**, a change to an approved spec that the user must confirm; all six roots are in range from the start. **H3 = a** the maw's Wraiths are ambush launches with a 3 s bottom-edge warning, at most 4 alive. **H4 = a** the phase-3 tear-free is a lift of the one boss (≈ 3 s, invulnerable, the core to `air`), the stump drawn from the boss's state. **H5 = a** the boss attacks `lob` and `sweep` are built on the Polyp Mortar's lob and the Mantis's sweep. **H10 = a** the HP stays as specified until the autopilot's fights are measured, then tuned to a ≈ 135 s mean inside 90–180 s with the par at the measured mean; the bounty falls from 720 (43 % of budget(14) = 1 687) to ≈ 506 as paid (≈ 30 %), in Act 1 terms ÷ (1.6 × the level's `bounty_scale`) in the data (the Harbour Kraken's convention). Corrected stale numbers: the size row (the concept's body is 440×440 and the roots spread ≈ 380 px, not 420 px) and the credits row. `design` goes to `review`; `implementation` is `in-progress`.
- 2026-10-12: M5 part H, step H4 (pending the user's review with H1–H10): the stat block and the boss script in `data.yaml`. **H10 = a measured**: with the spec's HP the autopilot's fight lasted ≈ 76 s at medium, so every part's HP was scaled ×1.8 (total 6,690 → 12,040: pods 665, mortar roots 775, maw 2,880, core 4,950), the measured mean is 132.4 s (five seeds 129.7–135.0 s) and **the par 132 s**. **The bounty is 372 in Act 1 terms (pods and mortar roots 31, maw 62, core 124: the spec's ratios), paid 452 = 26.8 % of budget(14)**, not ≈ 506 (30 %): with Level 14's `bounty_scale` 0.76 a 30 % share leaves a perfect run under the economy's 1.4 × budget (`TypicalHaulTest`), so the share is the most that keeps it (inside the act boss's 20–40 % tolerance). Settled while the user is away (pending): the part offsets from `assets/pivots/siege-spire.json` (the anchor 196 px below the top edge, the free station at x 240, y 120); the trunk an armoured part above the maw (shots glance off it; it is the stump), the maw before the core in the part list (a shot over both hits the maw); the core ×2 and the maw ×2 as part multipliers; the boss `lob` is the enemy `mortar` pattern (impact 40 px, the direct hit `heavy`), the `sweep` the enemy `laser-sweep` (120°, 1.2 s, 0.6 s telegraph, a beam 300 px long and 8 px wide from the core: out of reach of a ship low in the lower half, so "dodging in the open" means keeping low under the rings), both each every 2.5 s in turn (one every 5 s); the pods' orbs and the mortar roots' lobs `staggered`; the maw's window every 8 s, open 8 s (always open), its spawn an `ambush` launch with at most 4 alive; the sway (`sine` 100 px, 12.566 s) only after the lift.
- 2026-10-12: M5 part H, step H6 (the user is on vacation; **pending the user's review** in round 36):
  the presentation's own choices: the turret pods snap to the ship's bearing (32 headings, no turn
  rate); a mortar pod shows its open frame for 1/6 s from its lob, then its opening frame as long; the
  crown opens through its half-open frame over 0.5 s as the maw is exposed; the maw's bloom pulses
  faintly while it is open and flares through its four frames at each launch; the lift frames glide from
  the trunk's centre to the station; the free spire turns at 2 frames a second (7.5°/s, its 60°
  symmetry in 8 s); the tear-free shockwave is a ring of ten dust puffs (the root burst's) running out to
  170 px and sixteen slate shards over 1.5 s; the sweep's telegraph is a pulsing crimson line along its
  first edge with the far edge faint and its arc dotted, its beam a crimson band (twice its width as a
  glow) with a hot core; the death is its eight frames over the 3 s chain where it died, fourteen medium
  bursts spiralling out over it, three large blasts and the screen flash at the chain's end, then the
  credit shower. Pod 1's data offset sign fixed (see Level 14's Decisions): with the pod on its root the measured medium fight is 126.1–131.7 s, mean 129.5 s (was 132.4 s), so the par moves from 132 s to 130 s.
- 2026-10-12: M5 part H, step H7b (after the H7 capture; **pending the user's review**): the free
  spire's dark star, 76 px above the anchor at its station, merged with the stump under it. The stump
  is now drawn as muted ash (`SpireLooks.STUMP_ASH`: blended 60 % towards a grey, desaturated and low
  in contrast); darkening it made the two one dark mass. The free spire's station stays (par 130 s).
  Phase 3's two lines (Okafor's, then Rook's) now both play (Level 14's Decisions), and the canopy's
  withering sound waits for the death chain's end (3 s). The white screen flash at the chain's end is
  kept: it is every act boss's death flash (the Brood Carrier's too).
