---
title: Luna
design: approved
implementation: n/a
art: chosen
updated: 2026-10-03
---

# Luna

## Summary

The Moon: grey craters, pressure domes, the mass-driver tracks that launch ore into orbit, and
Shackleton Station at the south pole. Middle of Act 1 (L04–06); the act's Brood Carrier
battle follows at the Earth–Moon L1 point (see [earth-orbit](../earth-orbit/README.md)).

## Design

### Look & feel

Stark, silent, industrial. Flat grey regolith, long black shadows, domes glowing warm yellow.
Humanity's oldest colony, worn and patched. As the Vrell land, organic "roots" start to spread
across the craters. Touchstones: *Moon* (2009), Apollo photography, Tyrian's lunar stages.

### Sub-locations

| Sub-location | Description |
|---|---|
| Crater fields | Open regolith, rocks, rovers, isolated habitats. |
| Tranquility Base | The Apollo 11 heritage site on Mare Tranquillitatis and the convoy roads around it; the first Vrell brood pods land here. |
| Mass-driver line | A kilometres-long electromagnetic rail with launch sleds firing up the track. |
| Shackleton Station | The biggest lunar city: domes, landing pads, ice mines in permanent shadow. |
| Far side | Dark, radio-silent observatory arrays; the first Vrell nests grow here. |

### Parallax layers

| Layer | Contents |
|---|---|
| far | Only where the surface drops away below the ground layer, such as a rille floor (with haze). |
| ground | Regolith, craters, domes, roads, the mass-driver rail (sleds ride it before they climb); ground turrets, bunkers, Vrell nests. |
| low-air | Landing craft, low drones, dust plumes from impacts. |
| air | Player's plane. |
| high-air | Occasional ejected rock from impacts, launch sleds from the mass driver. |

The view looks straight down onto the surface, so there is **no `deep` layer** and no horizon:
the black sky, the stars and Earth never show. Earth is present only as its light, the blue
earthshine tint in the crater shadows (as in the chosen scene). Scroll factors from the scene:
`ground` 1.0, `low-air` 1.35, `high-air` 2.2.

**Roads.** A convoy road is level data, not tiles: a curve of `[t, x]` points (`t` when that
road point passes the middle of the screen, as for placed backdrop pieces), drawn by the game as
a textured ribbon on the ground layer (compacted regolith, two tyre tracks, orange edge posts as
in the scene). Vehicles that follow it (Level 04's crawlers) take their x and heading from it.
The road is 56 px wide, wide enough for the 40 px civilian crawler (the scene's road was 18–28 px).

### Palette & lighting

- Neutral grey surface; hard low-angle sunlight from the top-left makes long shadows.
- Warm yellow dome lights and orange hazard lights for contrast.
- Vrell growth introduces the first teal glow on the ground.

### Hazards & set pieces

- **Mass-driver sleds**: every few seconds a sled shoots up the track (a vertical line across
  the field) — telegraphed by lights along the rail; touching it hurts. (Signature set piece.)
- **Low gravity debris**: destroyed ground targets throw slow arcs of rock onto the air layer.
- **Dark craters**: permanently shadowed craters where only glowing enemies are visible.

### Natives

- Vrell: landers, ground nests that spawn crawlers, first ground turrets grown into rock.
- Coalition: overrun defence bunkers, rovers, domes to protect.

### Music mood

Tense and sparse at first (echoing synths, low pulse), building to the act-final battle with
the main theme in full.

### Chosen scene

The look of this setting is set by the chosen scenes in art direction: Luna (round 06) —
[sheet](../../art-direction/concept/scene-luna-r06-a.png), [scroll loop](../../art-direction/concept/scene-luna-r06-a.gif);
the far side (round 09) —
[sheet](../../art-direction/concept/scene-luna-farside-r09-a.png), [scroll loop](../../art-direction/concept/scene-luna-farside-r09-a.gif).
The concept files live in [art-direction](../../art-direction/README.md).

## Decisions

- 2026-09-30: Luna is the middle of Act 1 (L04–06); signature set piece: mass-driver sleds.
- 2026-09-30: Tranquility Base added as a sub-location (used by L04 *Tranquility Run*).
- 2026-10-01: Linked the chosen scene from art direction; `art: chosen`.
- 2026-10-01: Chosen far side scene linked (scene-luna-farside-r09-a, round 09).
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../reviews/acts-1-2/README.md).
- 2026-10-03: Layers and roads (user decisions, M4 part D): no `deep` layer on a top-down
  surface, Earth shows only as an earthshine tint; the mass-driver rail lies on the ground layer
  (as in the chosen scene scene-luna-r06-a); the road is level data, a curve of `[t, x]` points
  drawn as a textured ribbon on the ground layer that the crawlers follow, and it widens to fit
  the 40 px crawler. Chosen here (main-agent choice): the 56 px road width; `far` only for drops
  below the surface such as Level 04's rille floor; the scene's scroll factors.
