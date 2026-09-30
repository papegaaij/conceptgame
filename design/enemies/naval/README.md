---
title: Naval enemies
design: draft
implementation: not-started
art: none
updated: 2026-09-30
---

# Naval enemies

## Summary

Enemies on water: surface units (treated as `ground` layer targets on the water surface) and
submerged units on the `sub` layer. They appear on Earth's oceans and arctic in Act 2, and are
the core of the underwater Act 4 on Europa, where the `sub` layer becomes the play plane.

## Roster

| Name | Faction | Layer | Tier | Role/ability | Formations | First level | Design |
|---|---|---|---|---|---|---|---|
| Driftjelly | Vrell | ground (surface) / sub | small | Floating mine organism. `drift`s; pulses a `ring` when the player comes within 96 px. | swarm | 11 | idea |
| Reef Spitter | Vrell | ground (surface) | small | Barnacle gun grown on floating biomass rafts. `terrain` + 3-way `fan`. | turret nest | 11 | idea |
| Skimmer | Vrell | ground (surface) | small | Fast skiff that weaves between ice floes (`sine`) and fires `aimed` shots; comes from all edges. | convoy, cross | 13 | idea |
| Eel Swarm | Vrell | sub | large | A long snake of eel segments; each segment has to be destroyed, and the head fires `aimed` shots. | snake | 23 | idea |
| Abyss Ray | Vrell | sub | large | A large manta rising from the depths (it grows from a shadow); fires a wide `fan` wave, then glides away. | carrier + escorts | 24 | idea |
| Siren | Vrell | sub | medium | Sends out sonar `ring`s that do no damage but **slow** the player for 2 s. Dangerous in combination with others. | turret nest (in walls) | 25 | idea |
| Glow Angler | Vrell | sub | small | In darkness its lure looks like a pickup. When the player is close it lunges (`dive`) and fires a `ring`. | swarm (scattered) | 26 | idea |
| Depth Hunter | Unmarked (Helix Dynamics) | sub | medium | A human-built sub drone firing slow shootable `homing` torpedoes up the screen. **Hint 3.** | column | 27 | idea |
| Spiral Nautilus | Vrell | sub | medium | *(roster fork addition)* A coiled-shell cephalopod that rolls in its shell (`spin`, `radial` shell with a 16-angle tentacle crown) along `spiral-in` paths around the player, firing a `spiral` from its tentacles when it uncoils. The shell is `armoured`; shoot it while uncoiled. | circle (pairs orbiting in opposite directions) | 25 | idea |

## Design

- Above water (Act 2), `sub` units are shadows under the waves: hittable only by `anti-sub` and
  `area`, per the [layer rules](../README.md). Surfaced units are ground targets.
- Underwater (Act 4), the `sub` layer is the play plane: all weapons hit, but weapons without
  `anti-sub` do 50% damage. Enemy bullets and movement are slowed by the water. All numbers:
  [under water rules](../../world/europa/README.md#under-water-rules).

## Implementation

- [ ] Each enemy promoted to its own directory with a stat block before it is implemented.
- [ ] Surfacing/submerging transition visuals (shadow ⇄ sprite).

## Open questions

- Does the Glow Angler's fake pickup risk frustrating players? Proposal: its lure glows a
  slightly different colour than real pickups, and Varga warns about it on its first appearance.

## Decisions

- 2026-09-30: Roster of 8 naval enemies drafted.
- 2026-09-30: Converted to the 960×540 baseline (was 640×360).
- 2026-09-30: Enemy variety pass: size tier column added to every row; new units Spiral Nautilus (roster fork addition).
