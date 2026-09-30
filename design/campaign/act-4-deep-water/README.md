---
title: Act 4 – Deep Water
design: draft
implementation: not-started
art: none
updated: 2026-09-30
---

# Act 4 – Deep Water

## Summary

Levels 22–28. The Vrell are boring through Europa's ice to reach the ocean beneath. The
Stormhawk is fitted with a hydro-kit, and Aegis Wing follows them into the dark. This act is
the underwater act: the `sub` layer becomes the main play space and `anti-sub` weapons are
required. The evidence against Helix Dynamics piles up. The act ends against the Abyssal Maw,
and with the reveal: Ascendancy warships rising from the ice beside the Vrell.

## Roster

| # | Name | Setting | Layers | Directions | Density | Recommended traits | Introduces | Notes | Design |
|---|---|---|---|---|---|---|---|---|---|
| 22 | Ice Shelf | [europa](../../world/europa/README.md) – ice surface | ground, air | front | 3 | anti-ground, forward | Cracking-ice hazard (ground geysers); Vrell drill rigs as ground targets | Act opener and breather; Jupiter looms in the deep layer | idea |
| 23 | Through the Ice | [europa](../../world/europa/README.md) – ice shaft into the ocean | sub, ground | front | 3 | anti-sub, forward | **Underwater mode** (hydro-kit): slower bullets, drag on movement; [Eel Swarm](../../enemies/naval/README.md) | Dive down a Vrell drill shaft; `anti-sub` required from here | idea |
| 24 | Abyssal Plain | [europa](../../world/europa/README.md) – ocean floor | sub, ground (sea floor) | front | 4 | anti-sub, spread | [Abyss Ray](../../enemies/naval/README.md) rising from the depths | Wide open ocean floor with Vrell spawning reefs | idea |
| 25 | Siren Trench | [europa](../../world/europa/README.md) – trench | sub | sides, front | 4 | anti-sub, side | [Siren](../../enemies/naval/README.md) sonar pulses that slow the ship | Narrow trench with wall collision; enemies emerge from side caves | idea |
| 26 | Blackwater | [europa](../../world/europa/README.md) – deep ocean | sub | all | 3 | homing, area | **Darkness**: only the ship's light cone and bioluminescence; [Glow Angler](../../enemies/naval/README.md) | A stealth level: enemies are seen by their glow; lower density, high tension | idea |
| 27 | Hydrothermal Vents | [europa](../../world/europa/README.md) – vent field | sub, ground | front, rear | 4 | anti-sub, rear | [Depth Hunter](../../enemies/naval/README.md), a Helix Dynamics sub drone (**hint 3**); heat plumes as hazard | Varga identifies a Helix hull code for certain | idea |
| 28 | Abyssal Maw | [europa](../../world/europa/README.md) – Vrell hatchery | sub | all | 4 | anti-sub, piercing | Boss [Abyssal Maw](../../enemies/bosses/README.md) | **Twist reveal** at the end: Ascendancy cruisers break the ice alongside the Vrell fleet | idea |

## Design

### Synopsis

Europa's research town **Conamara Station** reports tremors. The Vrell are drilling through the
ice. Aegis Wing strikes the drill rigs on the surface, then dives through a drill shaft into the
ocean with hydro-kits fitted to their Stormhawks. Beneath the ice the Vrell are growing a
hatchery around an ancient thermal vent. The wing fights across the ocean floor, through a
trench and into black water. In the vent field they find Helix Dynamics submarines guarding
the hatchery. The Abyssal Maw, the hatchery's guardian, dies. As the wing surfaces,
Ascendancy cruisers in black and gold rise with the Vrell fleet and open fire on the UTC.

### Story beats

- L22: Europa is a scientific colony. The civilians include Varga's former colleagues.
- L23: the hydro-kit is a hasty field refit. Rook jokes about "flying a submarine".
- L24–26: human-built relay beacons pulse red-gold along the Vrell routes; Varga decodes Helix
  frequencies on them.
- L26: in the dark the Choir speaks more clearly: "*Your kin opened the door. Why do you fight?*"
  At the end of the level Rook is shot down and goes missing. He is absent (radio silent, escort
  slot limited to a heavy drone) for L27–L29; see [Rook](../../story/characters/rook/README.md).
- L27: **hint 3**. Varga confirms a Helix hull code. Okafor sends it to UTC High Command.
- L28: **the reveal**. As the wing surfaces, Ascendancy cruisers rise beside the Vrell fleet and
  open fire. Chairman Silas Vorne's system-wide broadcast, declaring the Jovian Ascendancy's
  independence and its alliance with the Vrell, opens Act 5 (L29 briefing).

### Settings

[Europa](../../world/europa/README.md): ice surface (L22), drill shaft (L23), ocean floor (L24),
trench (L25), deep water (L26), vent field (L27), hatchery (L28).

### New mechanics

- Underwater mode: the `sub` layer is the play plane, projectiles are slowed, and movement has
  drag (L23).
- Darkness with a light cone (L26).
- Heat plumes: vertical vent jets that damage the ship and deflect bullets (L27).

### Factions present

Vrell, plus Helix Dynamics sub drones (identified at L27, openly Ascendancy at the end of L28).

### Boss

[Abyssal Maw](../../enemies/bosses/README.md) (L28).

### Music

Act theme "Deep Water" (muffled, reverberant), a darkness variant for L26, and the reveal
sting; see [audio](../../audio/README.md).

## Implementation

- [ ] All 7 levels promoted to draft level documents and implemented.
- [ ] Underwater mode: movement drag, projectile speed modifier, visual filter.
- [ ] Darkness / light-cone rendering (L26).
- [ ] Hydro-kit refit is automatic for Act 4; underwater rules as in [europa](../../world/europa/README.md#under-water-rules).

## Open questions

- The hydro-kit is drafted as an automatic, free refit, with weapons without `anti-sub` doing
  50 % damage under water (rules in [europa](../../world/europa/README.md#under-water-rules)).
  The alternative is a hangar purchase that the intel panel insists on. Keep the automatic refit?

## Decisions

- 2026-09-30: Hydro-kit drafted as an automatic free refit; underwater rules owned by
  [europa](../../world/europa/README.md#under-water-rules).
- 2026-09-30: Rook goes missing at the end of L26 and returns in Act 5 (L30).
- 2026-09-30: The twist is revealed at the end of L28, with Vorne's broadcast in the level-29
  briefing.
