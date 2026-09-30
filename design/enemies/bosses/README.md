---
title: Bosses
design: draft
implementation: not-started
art: none
updated: 2026-09-30
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

## Implementation

- [ ] Each boss promoted to its own directory with phases, attack scripts and weak-point
      layout before it is implemented.
- [ ] Shared boss framework: multi-part hitboxes, phase transitions, HUD boss bar, intro and
      death sequences.

## Open questions

- Should the Ascendant's third phase (Vorne escaping) be skippable, or is it a scripted end?
  Proposal: scripted. The player can damage the lifeboat for bonus credits, but it always escapes.

## Decisions

- 2026-09-30: 7 act bosses and 5 mid-bosses drafted, plus a boss rush at L49.
