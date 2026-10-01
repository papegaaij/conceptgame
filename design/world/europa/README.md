---
title: Europa
design: draft
implementation: n/a
art: chosen
updated: 2026-10-01
---

# Europa

## Summary

Jupiter's ice moon: a cracked white surface, and beneath it a lightless ocean where the city of
Thera Deep farms the sea. The Stormhawk goes under water. Setting for Act 4, ending with the
Abyssal Maw — and Ascendancy ships opening fire.

## Design

### Look & feel

Cold, then dark, then alien. The surface is a white-blue plain of ridges and cracks with
Jupiter filling the sky. Under the ice: black water lit only by headlights, city lights and
bioluminescence; kelp-farm grids, pressure domes, hydrothermal vents. The Vrell are farming
something down here. Touchstones: *The Abyss* (1989), *Sphere*, Sub Culture (1997).

### Sub-locations

| Sub-location | Description |
|---|---|
| Ice surface | Ridges, cracks, drilling rigs, ice-breaker vehicles, Jupiter overhead. |
| Conamara Station | Research town on the Conamara Chaos ice field, above Thera Deep; the first to report the Vrell drilling. |
| The descent | Flying down a drilled shaft through the ice (transition level). |
| Thera Deep | The under-ice city: pressure domes, tube corridors, kelp farms. |
| Vent fields | Hydrothermal vents, black smokers, Vrell growth farms. |
| The trench | Deep, pitch-black trench; human-built beacons; the Abyssal Maw. |

### Parallax layers

| Layer | Contents |
|---|---|
| deep | Ice surface far below / ocean floor with vents and ridges. |
| ground | Domes, rigs, farms, turrets, Vrell growths on the sea floor. |
| sub | (Under water) the water column itself: drifting particles, schools of fish, light shafts, submerged enemies. |
| low-air / air | Surface: normal. Under water the player's plane is the "air" layer in water. |
| high-air | Surface: ice crystals and vapour plumes. Under water: the underside of the ice sheet with light through cracks. |

### Palette & lighting

- Surface: white-blue ice, orange-brown Jupiter looming, hard sunlight from top-left.
- Under water: near-black blue; light comes from the player's headlights (a soft cone ahead),
  city lights (warm) and bioluminescence (teal/violet — so Vrell bullets must be brighter and
  larger here to stay readable).
- Human beacons pulse red-gold — the visual hint of the Ascendancy.

### Hazards & set pieces

- **Under water rules**: see the table below. This is the setting where `anti-sub` weapons
  matter.
- **Currents**: zones that push the player and bullets sideways, shown by drifting particles.
- **Darkness**: the edges of the play field are dark; enemies light up only when close or
  firing.
- **Ice ceiling**: breaking through the ice from below at the end of a level. (Signature set
  piece: the descent and the breakout.)
- **Vent eruptions**: vents burst periodically, damaging anything above them.

### Under water rules

This table is the single definition of underwater play (levels 23–28). Other documents link
here instead of repeating the numbers. Values are first-draft balancing values.

| Rule | Value |
|---|---|
| Hydro-kit | Every Stormhawk (and Rook's variant-C Stormhawk) gets a free, automatic field refit before L23. It is not a hangar purchase; see [ship systems](../../player/systems/README.md). |
| Projectile speed | All bullets, the player's and the enemies', move at × 0.7. |
| Handling | Acceleration × 0.6 (drag, more inertia); top speed −15 %. |
| Shield | Regeneration −25 %. |
| Beams and lasers | Range halved. |
| Torpedo-type weapons | Range +50 %. |
| Damage | Weapons without the `anti-sub` trait do 50 % damage under water; `anti-sub` weapons do full damage. |
| Specials | No Airstrike or Orbital Lance under the ice; Sonar Pulse works only in water (see [specials](../../player/specials/README.md)). |
| Pressure hull | Optional utility module that removes the top-speed and shield-regeneration penalties (the drag stays); see [ship systems](../../player/systems/README.md). |

Which layers can be hit above water (the `sub` layer seen from the surface in Act 2) is part of
the layer rules in [enemies](../../enemies/README.md#layer-rules).

### Natives

- Vrell: swimmers, jellyfish-like floating mines, sub-layer ambushers, sea-floor growth farms.
- Ascendancy (late act): human beacons, then the first Ascendancy ships.
- Coalition: Thera Deep defences, submarines.

### Music mood

Slow, deep and eerie. Submerged pads, sonar pings, heartbeat bass; pressure building to the
reveal, which gets a sharp, shocking sting.

### Chosen scene

The look of this setting is set by the chosen parallax scene in art direction: Europa under water, kelp forest (round 07) —
[sheet](../../art-direction/concept/scene-europa-r07-a.png), [scroll loop](../../art-direction/concept/scene-europa-r07-a.gif).
The concept files live in [art-direction](../../art-direction/README.md).

## Decisions

- 2026-09-30: Europa as Act 4 with under-water rules; signature: the descent and ice breakout.
- 2026-09-30: Conamara Station added as a surface sub-location (Act 4 opening).
- 2026-09-30: Underwater rules consolidated here as the single owner: free automatic hydro-kit, 50 % damage for non-`anti-sub` weapons, optional pressure hull removes the speed/shield penalties.
- 2026-09-30: Rook's craft renamed from the placeholder "Kestrel" to his variant-C Stormhawk (see [wingmen](../../player/wingmen/README.md)).
- 2026-10-01: Linked the chosen scene from art direction; `art: chosen`.
