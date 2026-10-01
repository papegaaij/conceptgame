---
title: Earth
design: review
implementation: n/a
art: chosen
updated: 2026-10-01
---

# Earth

## Summary

Earth's surface, invaded. Megacities under Vrell siege, open ocean with Coalition naval fleets,
and the arctic where the Vrell land their heavy organisms. Setting for Act 2, ending with the
Siege Spire rooted in the heart of a megacity.

## Design

### Look & feel

The fight for home. Dense, lit megacities with elevated highways and arcologies; stormy grey
seas with carriers and oil platforms; blinding white arctic ice. Clouds everywhere — the first
setting with an atmosphere, so the first with weather. Touchstones: Raptor's city and sea
stages, *Ghost in the Shell* skylines, 90s naval disaster films.

### Sub-locations

| Sub-location | Description |
|---|---|
| Megacity: Nova Lagos (the Lagos–Accra sprawl) | Arcologies, highways, stadiums, rooftop landing pads; Vrell roots cracking streets. |
| Open ocean | Coalition fleet, carriers, oil and fusion platforms; Vrell swimmers breach the surface. |
| Coastal port | Docks, cranes, container stacks, harbour defences. |
| Arctic | Ice sheets, research bases, the Vrell landing zone with heavy organisms. |
| Geneva Concord | The UTC capital; the last level of the act, with the Siege Spire grown through its centre. |

### Parallax layers

| Layer | Contents |
|---|---|
| deep | Streets far below, ocean surface, ice sheet — the ground plane seen at altitude. |
| ground | Rooftops, ships on the sea, platforms, turrets, tanks, bunkers, Vrell roots. |
| low-air | Helicopters, low drones, smoke columns, searchlights. |
| air | Player's plane. |
| high-air | Clouds and smoke drifting over the player (can hide enemies briefly); rain streaks. |

### Palette & lighting

- Megacity: dusk and night — orange sodium lights, blue neon, fires; sun low from top-left.
- Ocean: slate grey-blue water, white foam, overcast diffuse light, lightning flashes.
- Arctic: white-blue, very bright, long cool shadows; Vrell glow stands out strongly.
- Avoid saturated teal in city neon so Vrell bullets stay readable.

### Hazards & set pieces

- **Cloud cover**: high-air clouds obscure part of the field for a moment; enemies may hide
  under them.
- **Naval broadsides**: friendly carriers fire flak into the sky — harmless to the player but a
  visual cue; overrun ships fire at the player.
- **Collapsing arcology**: a tower falls across the ground layer, destroying enemy tanks.
- **The Spire's roots**: in the last level, roots burst out of the ground and become targets.
  (Signature set piece.)
- **Whiteout**: arctic blizzard reduces visibility near the edges of the play field.

### Natives

- Vrell: ground walkers, root turrets, swimmers and breaching sea organisms, heavy landers.
- Coalition: fleet ships, city defences, tanks (allied, sometimes overrun).

### Music mood

Bigger, more desperate. Heavier drums, choir pads, the main theme in a minor key during the
city levels.

### Chosen scene

The look of each sub-location is set by its chosen scene in art direction:

| Sub-location | Round | Files |
|---|---|---|
| Megacity at night | 03 | [sheet](../../art-direction/concept/parallax-r03-b.png), [scroll loop](../../art-direction/concept/parallax-r03-b.gif) |
| Open ocean | 10 | [sheet](../../art-direction/concept/scene-ocean-r10-a.png), [scroll loop](../../art-direction/concept/scene-ocean-r10-a.gif) |
| Ocean storm | 10 | [sheet](../../art-direction/concept/scene-storm-r10-a.png), [scroll loop](../../art-direction/concept/scene-storm-r10-a.gif) |
| Arctic | 09 | [sheet](../../art-direction/concept/scene-arctic-r09-a.png), [scroll loop](../../art-direction/concept/scene-arctic-r09-a.gif) |
| Geneva Concord (capital) | 10 | [sheet](../../art-direction/concept/scene-geneva-r10-a.png), [scroll loop](../../art-direction/concept/scene-geneva-r10-a.gif) |

The concept files live in [art-direction](../../art-direction/README.md).

## Decisions

- 2026-09-30: Earth as Act 2 setting with city, ocean and arctic; signature: Siege Spire roots.
- 2026-09-30: City names Nova Lagos (megacity) and Geneva Concord (UTC capital) adopted from the campaign.
- 2026-10-01: Linked the chosen scene from art direction; `art: chosen`.
- 2026-10-01: Chosen scenes linked for every sub-location: ocean (scene-ocean-r10-a), storm (scene-storm-r10-a), arctic (scene-arctic-r09-a) and Geneva (scene-geneva-r10-a), next to the megacity (parallax-r03-b).
