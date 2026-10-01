---
title: Vrell space
design: draft
implementation: n/a
art: chosen
updated: 2026-10-01
---

# Vrell space

## Summary

The Tether Gate and what lies beyond it: space that is alive — nebulae of spores, hive
structures the size of moons, and dead Vrell worlds that something emptied. Setting for Act 7,
ending with the Choir Heart.

## Design

### Look & feel

Beautiful, alien and wrong. The Tether Gate is a vast ring of dark stone-like material with
glowing glyphs. On the far side: violet and teal nebulae, bioluminescent hive-reefs, living
bridges between asteroids, the Choir Heart pulsing at the centre. The further in, the more
signs of death: withered hives, grey husks, silence. Touchstones: *Event Horizon*, H.R. Giger
(toned down), R-Type's organic stages, Darius's giant bio-bosses.

### Sub-locations

| Sub-location | Description |
|---|---|
| The Tether Gate | The ring itself: flying along its surface and through the event plane. |
| Spore nebula | Dense clouds of glowing spores; hive-reefs. |
| Hive-reef | Moon-sized living structures: flying over and inside them. |
| Dead worlds | Withered, grey Vrell worlds — the hint of the Silence. Eerily quiet levels. |
| The Choir Heart | The core organism of the hive mind; final battle. |

### Parallax layers

| Layer | Contents |
|---|---|
| deep | Nebula clouds, distant hive-reefs, strange stars; in dead worlds a starless dark patch. |
| ground | Hive surface: chitin plates, glowing veins, spawning pits, living turrets. |
| low-air | Tendrils, floating spore sacs, small organisms drifting. |
| air | Player's plane. |
| high-air | Spore clouds and tendrils passing over the player. |

### Palette & lighting

- Violet, deep purple and teal nebulae; organic pinks and dark chitin brown.
- No clear sun: light comes from bioluminescence and the nebula glow, so the top-left key
  light becomes a soft violet glow (keep the direction for consistent sprites).
- Danger: Vrell bullets are the same hues as the background here — make them larger, brighter
  and outlined (see [art-direction](../../art-direction/README.md)).
- Dead worlds: desaturated grey-brown with a single cold white light.

### Hazards & set pieces

- **Living terrain**: hive surfaces that open and close; spawning pits release enemies.
- **Spore clouds**: slow the player and hide bullets; burn them away with `area` weapons.
- **Gate transit**: flying through the Tether Gate's event plane — the screen warps, colours
  invert briefly. (Signature set piece.)
- **The quiet**: in the dead worlds, the music drops out and nothing attacks for a while —
  then something does.

### Natives

- Vrell: every kind, including elite and ancient variants; hive-structures as terrain.
- Ascendancy remnants: hybrids that went native, if the Act 6 story leaves any.

### Music mood

Alien and overwhelming, then desolate. Choir voices, reversed textures, the main theme
transformed; the dead worlds nearly silent; the final battle all themes at once.

### Chosen scene

The look of this setting is set by the chosen parallax scene in art direction: Vrell space (round 06) —
[sheet](../../art-direction/concept/scene-vrell-space-r06-a.png), [scroll loop](../../art-direction/concept/scene-vrell-space-r06-a.gif).
The concept files live in [art-direction](../../art-direction/README.md).

## Decisions

- 2026-09-30: Vrell space as Act 7; signature: transit through the Tether Gate.
- 2026-10-01: Linked the chosen scene from art direction; `art: chosen`.
