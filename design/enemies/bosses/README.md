---
title: Bosses
design: approved
implementation: done
art: chosen
updated: 2026-10-09
---

# Bosses

## Summary

Seven act bosses end each act; five mid-bosses break up the longer stretches. Every boss is a
multi-part set piece that tests the mechanics its act introduced. Level 49 is a boss rush of
shortened re-creations of earlier bosses.

## Contents

Promoted to their own documents for the Acts 1–2 wrap-up; the remaining units stay in the roster below.

| Part | Summary | Design | Impl | Art |
|---|---|---|---|---|
| [gorgon-frigate](gorgon-frigate/README.md) | Act 1 mid-boss: medusa-bell warship with three serpent-neck turrets (L05) | approved | done | final |
| [brood-carrier](brood-carrier/README.md) | Act 1 boss: living carrier, overhead pass, broadside bays, core (L07) | approved | done | final |
| [harbour-kraken](harbour-kraken/README.md) | Act 2 mid-boss: cephalopod around a platform, lane slams, surfacing head (L11) | approved | done | final |
| [siege-spire](siege-spire/README.md) | Act 2 boss: rooted citadel that tears free and rises (L14) | approved | not-started | chosen |

## Roster

### Act bosses

| Name | Faction | Layer | Role/ability | Formations | First level | Design |
|---|---|---|---|---|---|---|
| Dust Colossus | Vrell | ground (burrowing) | A colony-sized sand organism that `burrow`s under the arena and breaches from the sides, the rear and the front in turn. Each breach exposes a mouth (weak point) that fires a `ring` and sucks debris toward it. Tests side and rear weapons. | – | 21 | idea |
| Abyssal Maw | Vrell | sub | Underwater leviathan guarding the hatchery. Rises from below in a jaw-snap (a telegraphed shadow), trails Eel Swarms, and fires bioluminescent `fan` walls. Wrecked Helix implants on its body are the last clue before the reveal. | carrier + escorts | 28 | idea |
| Iron Sovereign | Ascendancy | space + ground | A battle station with three concentric rotating rings. Each ring carries batteries (rail, SAM, laser) that rotate into view; destroy the batteries to stop a ring, and stop all rings to expose the core. Tests piercing and positioning. | turret nest | 35 | idea |
| Ascendant | Hybrid | air + space | Vorne's flagship, half warship and half Vrell organism. **Phase 1**: gold hull with shield generators and mirror panels (`reflect`). **Phase 2**: the Vrell half erupts and regenerates tissue while spawning Chimeras. **Phase 3**: Vorne's Vrell lifeboat breaks free and flees (can't be killed; it escapes). Taunts from Vorne over the radio. | carrier + escorts | 42 | idea |
| Choir Heart | Vrell | all | The hive mind's core: a vast luminous organ in the hive's centre. It switches between the attack styles of every Vrell enemy in the game, reshapes the arena (walls of tissue, darkness, distorted scroll) and speaks through the Choir. The final phase is a single weak point in a storm of bullets. | – | 50 | idea |

### Mid-bosses

| Name | Faction | Layer | Role/ability | Formations | First level | Design |
|---|---|---|---|---|---|---|
| Revenant Walker | Unmarked (Ascendancy) | ground | A human-built war walker with filed-off serials. Missile pods (`homing`) and a rear-facing flamer that attacks after you pass. **Hint 2.** | – | 19 | idea |
| Honour Guard | Ascendancy | air | Three ace pilots in custom gold Talons attacking from front, sides and rear in turn. Each ace has its own pattern (sniper `laser-line`, `spiral` dancer, `kamikaze` feints) and taunts over the radio. | pincer, rear ambush | 40 | idea |
| Vorne's Chimera | Hybrid | air + ground | Vorne fused with his Vrell lifeboat inside a Vrell-grown citadel. Alternates between Ascendancy tactics (shields, missiles) and Vrell ones (spawning, regeneration). Pulp villain monologue; a tragic last line. | – | 48 | idea |

### Boss rush (L49 *Echoes*)

Shortened, single-phase "echo" versions of the Brood Carrier, Dust Colossus and Iron Sovereign,
re-created by the Choir. Armour and specials refill between fights. These re-use the original
boss assets with a spectral tint, not new designs.

## Design

- Every boss has a **health bar** with its name at the top of the play field ([HUD](../../ui/hud/README.md#in-the-play-field); a mid-boss's bar is shorter), visible **phases** (a distinct change in
  look and pattern), and weak points marked by glow (Vrell) or exposed machinery (Ascendancy).
- Bosses use only attack patterns from the [vocabulary](../README.md), combined; they add no
  new hidden rules.
- Every boss fight has a short **intro** (the boss enters, a radio line, its name on the HUD)
  and a large **death sequence** (chain explosions, screen flash) followed by a credit shower.
- Target duration: act bosses 90–180 s at medium, mid-bosses 45–75 s. Every boss and mid-boss
  has a **par** time in its data; a kill under par pays the
  [Boss rush](../../systems/scoring/README.md#level-end-bonuses) bonus.
- A boss's stat block and script (parts, neck or limb chains, phases, par) live in its
  `data.yaml` ([schema](../../tech/architecture/README.md#data-file-schemas)); the first is the
  [Gorgon Frigate](gorgon-frigate/README.md) (M4 part E).

## Concept art

Concept [round 03](../../concept-rounds/round-03/README.md). Prompts: [concept/prompts.md](concept/prompts.md); generator `tools/concept/enemies_r03.py`.

| File | What | Status |
|---|---|---|
| [concept/brood-carrier-r03-a.png](concept/brood-carrier-r03-a.png) | Brood Carrier (Act 1 boss, language A): full 288×626 sprite with labelled parts and weak points, phase 2 broadside in the play field, bay and core iris closed/open details | superseded by the r04 colour pass |

Concept [round 04](../../concept-rounds/round-04/README.md) — colour pass on the chosen enemies with the [role colours](../../README.md#role-colours) (chitin base = role family, glow = kind of threat; Ascendancy black & gold with a per-unit accent and a thin gold/red rim light). Same models and sheet layout; generator `tools/concept/enemies_r04.py`.

| File | What | Status |
|---|---|---|
| [concept/brood-carrier-r04-a.png](concept/brood-carrier-r04-a.png) | Brood Carrier — teal-black chitin with teal veins; bay sacs and core (weak points) in contrasting lime | chosen |

Concept [round 06](../../concept-rounds/round-06/README.md) — generator `tools/concept/bosses_r06.py`; all weak points glow lime like the Brood Carrier.

| File | What | Status |
|---|---|---|
| [concept/gorgon-frigate-r06-a.png](concept/gorgon-frigate-r06-a.png) | Gorgon Frigate (Act 1 mid-boss) — medusa-bell warship with three serpent-neck turrets, phases: turrets → stumps → crown opens over the core (sheet) | chosen |
| [concept/gorgon-frigate-r06-a.gif](concept/gorgon-frigate-r06-a.gif) | Gorgon Frigate (Act 1 mid-boss) — medusa-bell warship with three serpent-neck turrets, phases: turrets → stumps → crown opens over the core (motion) | chosen |
| [concept/rejected/harbour-kraken-r06-a.png](concept/rejected/harbour-kraken-r06-a.png) | Harbour Kraken (Act 2 mid-boss) — cephalopod wrapped around an offshore platform, telegraphed arm slams, head surfaces to fire fans (sheet) | rejected — doesn't read as rising from the depths; redo (round 07) |
| [concept/rejected/harbour-kraken-r06-a.gif](concept/rejected/harbour-kraken-r06-a.gif) | Harbour Kraken (Act 2 mid-boss) — cephalopod wrapped around an offshore platform, telegraphed arm slams, head surfaces to fire fans (motion) | rejected — doesn't read as rising from the depths; redo (round 07) |
| [concept/siege-spire-r06-a.png](concept/siege-spire-r06-a.png) | Siege Spire (Act 2 boss) — rooted citadel with turret and mortar roots, Wraith-launching maw; tears free and rises in its last phase (sheet) | chosen |
| [concept/siege-spire-r06-a.gif](concept/siege-spire-r06-a.gif) | Siege Spire (Act 2 boss) — rooted citadel with turret and mortar roots, Wraith-launching maw; tears free and rises in its last phase (motion) | chosen |

Concept [round 07](../../concept-rounds/round-07/README.md) — Kraken redone as a creature from the depths; generator `tools/concept/kraken_r07.py`.

| File | What | Status |
|---|---|---|
| [concept/harbour-kraken-r07-a.png](concept/harbour-kraken-r07-a.png) | Harbour Kraken r07 — one connected animal: arms continue under water to the mantle (four depth bands), foam collars where arms break the surface, swell-band water, top-down surfacing and diving, slam sequence, phases (sheet) | chosen |
| [concept/harbour-kraken-r07-a.gif](concept/harbour-kraken-r07-a.gif) | Harbour Kraken r07 — 10.4 s loop at 15 fps: churning lane telegraph, arm rises and slams with spray, head surfaces crown-first, lime eyes, 7-orb fan, dives | chosen |

## Implementation

- [x] Each boss promoted to its own directory with phases, attack scripts and weak-point
      layout before it is implemented: the Acts 1–2 bosses (Gorgon Frigate and Brood Carrier,
      built in M4; Harbour Kraken and Siege Spire for M5).
- [ ] The same for the roster's bosses of Acts 3–7 — **later: Act 3** (the Revenant Walker, L19,
      and the Dust Colossus, L21, first) to Act 7.
- [x] Shared boss framework: multi-part hitboxes, neck chains, phase transitions, HUD boss bar,
      the intro descent, the chained death and the credit shower (M4 part E, with the Gorgon
      Frigate; drawn in plain shapes until its production sprites).
- [x] The intro's sting (with Level 05's music) and the death's screen flash (an act boss's
      only, with the Brood Carrier in M4 part G).

## Open questions

- Should the Ascendant's third phase (Vorne escaping) be skippable, or is it a scripted end?
  Proposal: scripted. The player can damage the lifeboat for bonus credits, but it always escapes.

## Decisions

- 2026-09-30: 7 act bosses and 5 mid-bosses drafted, plus a boss rush at L49.
- 2026-09-30: Concept round 03: Brood Carrier mockup (language A) with launch bays, bay sacs and core iris as marked weak points.
- 2026-09-30: Concept round 04: colour pass on the Brood Carrier with the role colours (r04 proposals).
- 2026-10-01: Concept round 06: Gorgon Frigate and Siege Spire chosen. Harbour Kraken rejected: the arms look detached, the waves are plain circles, it pops up instead of surfacing top-down, and the arms' submerged parts and splash where they enter the water are missing — redo in round 07.
- 2026-10-01: Concept round 07: the redone Harbour Kraken chosen ("great").
- 2026-10-01: Brood Carrier length: about one screen, as in the chosen concept (the roster's "two screens" is dropped).
- 2026-10-01: Acts 1–2 units promoted to full specs: Gorgon Frigate, Brood Carrier, Harbour Kraken, Siege Spire.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../reviews/acts-1-2/README.md).
- 2026-10-03: M4 part E: the boss bar sits at the top of the play field, as the HUD document has
  it (the "side HUD" here was a slip; main-agent choice); mid-bosses get a par and earn the Boss
  rush bonus (user decision); the boss data schema is planned with the Gorgon Frigate.
- 2026-10-03: M4 part E: the shared boss framework is built with the Gorgon Frigate (a set piece
  with a boss script, see the [architecture](../../tech/architecture/README.md) log); the sting
  comes with Level 05's data, the screen flash and the sprites with the production art.
- 2026-10-05: M4 close-out bookkeeping (round 26): the promotion item is split into the Acts 1–2 bosses, all promoted (two built in M4), and the roster's Acts 3–7 bosses, tagged with their acts; every other item is ticked, so the document is `done` for M4.
