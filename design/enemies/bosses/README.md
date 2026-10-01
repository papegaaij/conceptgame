---
title: Bosses
design: draft
implementation: not-started
art: proposed
updated: 2026-10-01
---

# Bosses

## Summary

Seven act bosses end each act; five mid-bosses break up the longer stretches. Every boss is a
multi-part set piece that tests the mechanics its act introduced. Level 49 is a boss rush of
shortened re-creations of earlier bosses.

## Roster

### Act bosses

| Name | Faction | Layer | Role/ability | Formations | First level | Design |
|---|---|---|---|---|---|---|
| Brood Carrier | Vrell | high-air → air | A living carrier the length of two screens. **Phase 1**: its hull scrolls past overhead (`high-air`) while launch bays `spawn` Skitters and Needlers. **Phase 2**: it turns broadside, and glowing bay sacs (weak points) open between `fan` volleys. **Phase 3**: the exposed core fires `spiral`s. Tests piercing and target priority. | carrier + escorts | 07 | idea |
| Siege Spire | Vrell | ground + air | A citadel rooted in the capital. Ground-layer root turrets (`aimed`, `mortar`) protect a central spire that launches Wraiths. When the roots die, the spire tears itself free and becomes airborne for a final `laser-sweep` phase. Tests anti-ground and rear awareness. | turret nest | 14 | idea |
| Dust Colossus | Vrell | ground (burrowing) | A colony-sized sand organism that `burrow`s under the arena and breaches from the sides, the rear and the front in turn. Each breach exposes a mouth (weak point) that fires a `ring` and sucks debris toward it. Tests side and rear weapons. | – | 21 | idea |
| Abyssal Maw | Vrell | sub | Underwater leviathan guarding the hatchery. Rises from below in a jaw-snap (a telegraphed shadow), trails Eel Swarms, and fires bioluminescent `fan` walls. Wrecked Helix implants on its body are the last clue before the reveal. | carrier + escorts | 28 | idea |
| Iron Sovereign | Ascendancy | space + ground | A battle station with three concentric rotating rings. Each ring carries batteries (rail, SAM, laser) that rotate into view; destroy the batteries to stop a ring, and stop all rings to expose the core. Tests piercing and positioning. | turret nest | 35 | idea |
| Ascendant | Hybrid | air + space | Vorne's flagship, half warship and half Vrell organism. **Phase 1**: gold hull with shield generators and mirror panels (`reflect`). **Phase 2**: the Vrell half erupts and regenerates tissue while spawning Chimeras. **Phase 3**: Vorne's Vrell lifeboat breaks free and flees (can't be killed; it escapes). Taunts from Vorne over the radio. | carrier + escorts | 42 | idea |
| Choir Heart | Vrell | all | The hive mind's core: a vast luminous organ in the hive's centre. It switches between the attack styles of every Vrell enemy in the game, reshapes the arena (walls of tissue, darkness, distorted scroll) and speaks through the Choir. The final phase is a single weak point in a storm of bullets. | – | 50 | idea |

### Mid-bosses

| Name | Faction | Layer | Role/ability | Formations | First level | Design |
|---|---|---|---|---|---|---|
| Gorgon Frigate | Vrell | air | A medium ship with three tentacle turrets that fire `aimed` bursts; each turret can be destroyed separately. The first multi-part enemy. | carrier + escorts | 05 | idea |
| Harbour Kraken | Vrell | ground (surface) + sub | A Vrell organism wrapped around a harbour platform. Tentacles rise from the water (`sub` → surface) to slam lanes; its head surfaces to fire. | – | 11 | idea |
| Revenant Walker | Unmarked (Ascendancy) | ground | A human-built war walker with filed-off serials. Missile pods (`homing`) and a rear-facing flamer that attacks after you pass. **Hint 2.** | – | 19 | idea |
| Honour Guard | Ascendancy | air | Three ace pilots in custom gold Talons attacking from front, sides and rear in turn. Each ace has its own pattern (sniper `laser-line`, `spiral` dancer, `kamikaze` feints) and taunts over the radio. | pincer, rear ambush | 40 | idea |
| Vorne's Chimera | Hybrid | air + ground | Vorne fused with his Vrell lifeboat inside a Vrell-grown citadel. Alternates between Ascendancy tactics (shields, missiles) and Vrell ones (spawning, regeneration). Pulp villain monologue; a tragic last line. | – | 48 | idea |

### Boss rush (L49 *Echoes*)

Shortened, single-phase "echo" versions of the Brood Carrier, Dust Colossus and Iron Sovereign,
re-created by the Choir. Armour and specials refill between fights. These re-use the original
boss assets with a spectral tint, not new designs.

## Design

- Every boss has a **health bar** in the side HUD, visible **phases** (a distinct change in
  look and pattern), and weak points marked by glow (Vrell) or exposed machinery (Ascendancy).
- Bosses use only attack patterns from the [vocabulary](../README.md), combined; they add no
  new hidden rules.
- Every boss fight has a short **intro** (the boss enters, a radio line, its name on the HUD)
  and a large **death sequence** (chain explosions, screen flash) followed by a credit shower.
- Target duration: act bosses 90–180 s at medium, mid-bosses 45–75 s.

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
| [concept/gorgon-frigate-r06-a.png](concept/gorgon-frigate-r06-a.png) | Gorgon Frigate (Act 1 mid-boss) — medusa-bell warship with three serpent-neck turrets, phases: turrets → stumps → crown opens over the core (sheet) | proposed |
| [concept/gorgon-frigate-r06-a.gif](concept/gorgon-frigate-r06-a.gif) | Gorgon Frigate (Act 1 mid-boss) — medusa-bell warship with three serpent-neck turrets, phases: turrets → stumps → crown opens over the core (motion) | proposed |
| [concept/harbour-kraken-r06-a.png](concept/harbour-kraken-r06-a.png) | Harbour Kraken (Act 2 mid-boss) — cephalopod wrapped around an offshore platform, telegraphed arm slams, head surfaces to fire fans (sheet) | proposed |
| [concept/harbour-kraken-r06-a.gif](concept/harbour-kraken-r06-a.gif) | Harbour Kraken (Act 2 mid-boss) — cephalopod wrapped around an offshore platform, telegraphed arm slams, head surfaces to fire fans (motion) | proposed |
| [concept/siege-spire-r06-a.png](concept/siege-spire-r06-a.png) | Siege Spire (Act 2 boss) — rooted citadel with turret and mortar roots, Wraith-launching maw; tears free and rises in its last phase (sheet) | proposed |
| [concept/siege-spire-r06-a.gif](concept/siege-spire-r06-a.gif) | Siege Spire (Act 2 boss) — rooted citadel with turret and mortar roots, Wraith-launching maw; tears free and rises in its last phase (motion) | proposed |

## Implementation

- [ ] Each boss promoted to its own directory with phases, attack scripts and weak-point
      layout before it is implemented.
- [ ] Shared boss framework: multi-part hitboxes, phase transitions, HUD boss bar, intro and
      death sequences.

## Open questions

- **Brood Carrier length**: the roster says "the length of two screens"; the round-03 mockup is 288×626 (about one screen). Scale it up about 1.7× for phase 1, or keep one screen so the broadside of phase 2 still fits? Proposal: keep one screen.
- Should the Ascendant's third phase (Vorne escaping) be skippable, or is it a scripted end?
  Proposal: scripted. The player can damage the lifeboat for bonus credits, but it always escapes.

## Decisions

- 2026-09-30: 7 act bosses and 5 mid-bosses drafted, plus a boss rush at L49.
- 2026-09-30: Concept round 03: Brood Carrier mockup (language A) with launch bays, bay sacs and core iris as marked weak points.
- 2026-09-30: Concept round 04: colour pass on the Brood Carrier with the role colours (r04 proposals).
