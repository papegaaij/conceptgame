---
title: Act 3 – Red Dust
design: draft
implementation: not-started
art: none
updated: 2026-09-30
---

# Act 3 – Red Dust

## Summary

Levels 15–21. The Mars colonies are under siege. Aegis Wing descends through the dust, runs the
Valles Marineris canyons and defends the domes. The first hints of human involvement turn up
here: unmarked drones flying with the Vrell, and a human-built war machine. The act introduces
burrowing enemies, canyon walls and the first reverse-scroll level. It ends against the Dust
Colossus in the Hellas basin.

## Roster

| # | Name | Setting | Layers | Directions | Density | Recommended traits | Introduces | Notes | Design |
|---|---|---|---|---|---|---|---|---|---|
| 15 | Olympus Descent | [mars](../../world/mars/README.md) – orbit to surface | ground, low-air, air | front | 3 | forward, anti-ground | [Burrower](../../enemies/ground/README.md) (only hittable when surfaced) | Opens with a descent through the atmosphere (clouds scroll past in the foreground), then the slopes of Olympus Mons | idea |
| 16 | Valles Canyon Run | [mars](../../world/mars/README.md) – canyon | ground, low-air | front | 4 | forward, piercing, beam | Canyon walls (terrain collision); [Tendril Anchor](../../enemies/ground/README.md) barriers | Fast scroll, narrowing and branching canyon; `beam` burns through tendrils | idea |
| 17 | Dome Siege | [mars](../../world/mars/README.md) – colony domes | air, ground | front, sides | 4 | spread, anti-ground | [Choir Herald](../../enemies/air/README.md) (shield aura for nearby enemies) | Secondary objective: no bomber reaches the domes | idea |
| 18 | Dust Storm | [mars](../../world/mars/README.md) – plains | air, low-air | all | 3 | homing, spread | [Ghost Drone](../../enemies/air/README.md), an unmarked human-built drone (**hint 1**); visibility drops to a radius around the ship | Varga: "*That's not Vrell. That signature is... ours?*" | idea |
| 19 | Foundry of Tharsis | [mars](../../world/mars/README.md) – industrial zone | ground, air | front, rear | 4 | anti-ground, rear | Mid-boss [Revenant Walker](../../enemies/bosses/README.md), an unmarked human war machine (**hint 2**) | Conveyor belts and furnace vents on the ground layer; turrets fire from behind as you pass | idea |
| 20 | Retreat from Hellas | [mars](../../world/mars/README.md) – Hellas rim | air, ground | rear | 4 | rear, homing | **Reverse scroll**: the screen scrolls downward while a Vrell swarm pursues from the bottom edge | The ship faces up but flies "backwards"; rear guns are essential | idea |
| 21 | Dust Colossus | [mars](../../world/mars/README.md) – Hellas basin | ground, low-air | all | 4 | anti-ground, side, rear | Boss [Dust Colossus](../../enemies/bosses/README.md), surfacing anywhere around the play field | The arena scrolls slowly; the boss breaches from the sides and from behind | idea |

## Design

### Synopsis

The Mars colonies of **Ares Landing** and **Tharsis Foundry** are cut off. Aegis Wing makes a
contested descent and clears the canyons that link the colonies. They hold Ares Landing's domes
while the population is sheltered below ground. During a dust storm the wing meets drones that
carry no Vrell biology at all, and in the Tharsis foundry they fight a walker built with human
engineering. The Vrell counter-attack forces a retreat to the Hellas rim, where the Dust
Colossus, a colony-sized burrowing organism, rises out of the basin.

### Story beats

- L15: Mars asks for help. The colonists are proud and resent Earth arriving late.
- L16–17: the Vrell sweep past the Helix compound without touching it (see
  [mars](../../world/mars/README.md)). Varga notices; Okafor tells her to sit on it.
- L18: **hint 1**. Varga finds a human-built drone among the Vrell. Okafor again orders it kept quiet.
- L19: **hint 2**. The Revenant Walker's wreck has its serial plates filed off. Its alloy
  matches Helix Dynamics foundry stock.
- L20: a genuine retreat. The war is not going well.
- L21: the Colossus falls and Mars is saved. The Vrell pull back toward Jupiter's moons, and
  Varga tracks their fleet to Europa.

### Settings

[Mars](../../world/mars/README.md): orbit and Olympus Mons (L15), Valles Marineris (L16), Ares
Landing domes (L17), plains (L18), Tharsis foundry (L19), Hellas basin (L20–21).

### New mechanics

- Burrowing enemies with a hittable window (L15).
- Terrain collision with canyon walls; high-speed scroll; branching paths (L16).
- Support enemies that buff others (L17).
- Reduced visibility radius (L18).
- Reverse scroll (L20).

### Factions present

Vrell, plus **unmarked** Ascendancy machines (Ghost Drone, Revenant Walker). Nothing identifies
them as Ascendancy yet.

### Boss

[Dust Colossus](../../enemies/bosses/README.md) (L21); mid-boss
[Revenant Walker](../../enemies/bosses/README.md) (L19).

### Music

Act theme "Red Dust", boss theme; see [audio](../../audio/README.md).

## Implementation

- [ ] All 7 levels promoted to draft level documents and implemented.
- [ ] Terrain collision for canyon walls (L16) — rules in [art-direction](../../art-direction/README.md) layer model.
- [ ] Reverse-scroll mode (L20).
- [ ] Visibility-radius effect (L18).

## Open questions

- How hard should canyon wall contact hit: damage plus push-back (proposal), or instant death?

## Decisions

- 2026-09-30: The twist hints are the Ghost Drone (L18) and the Revenant Walker (L19).
