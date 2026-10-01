---
title: World
design: approved
implementation: n/a
art: chosen
updated: 2026-10-01
---

# World

## Summary

The places the war is fought: eight settings, from Earth orbit to the far side of the Tether
Gate. Each setting defines the look, the parallax layers, the hazards and the mood that the
levels of its act build on. Levels reference a setting; they do not redefine it.

## Contents

| Part | Summary | Design | Impl | Art |
|---|---|---|---|---|
| [earth-orbit](earth-orbit/README.md) | Shipyards, orbital defence platforms and debris above a blue Earth | approved | n/a | chosen |
| [luna](luna/README.md) | Grey craters, Shackleton Station and mass-driver tracks | approved | n/a | chosen |
| [earth](earth/README.md) | Megacities, open ocean and arctic on Earth's surface | approved | n/a | chosen |
| [mars](mars/README.md) | Red canyons, dust storms, terraforming domes, Olympus Mons | draft | n/a | chosen |
| [europa](europa/README.md) | Cracked ice surface and the dark ocean beneath it | draft | n/a | chosen |
| [belt](belt/README.md) | Asteroid fields and hollowed-out mining stations | draft | n/a | chosen |
| [jovian](jovian/README.md) | Jupiter's storms, floating cloud cities, Callisto HQ | draft | n/a | chosen |
| [vrell-space](vrell-space/README.md) | The Tether Gate and the living, dying Vrell worlds beyond | draft | n/a | chosen |

## Design

### Settings per act

| Act | Setting(s) | Levels |
|---|---|---|
| 1 First Contact | earth-orbit, luna | 01–07 |
| 2 Homefront | earth | 08–14 |
| 3 Red Dust | mars | 15–21 |
| 4 Deep Water | europa | 22–28 |
| 5 The Belt | belt | 29–35 |
| 6 Jovian Storm | jovian | 36–42 |
| 7 Beyond the Gate | vrell-space | 43–50 |

The exact assignment of levels to sub-locations is in [campaign](../campaign/README.md).

### What a setting document contains

Every setting README has these sections under **Design**:

1. **Look & feel** – one paragraph that sets the scene, plus reference touchstones.
2. **Sub-locations** – the distinct areas levels can take place in.
3. **Parallax layers** – what is drawn on each layer used by this setting (`deep`, `ground`,
   `low-air`, `air`, `high-air`, `sub`, `space`; layer model in
   [art-direction](../art-direction/README.md)).
4. **Palette & lighting** – dominant colours, light direction and quality, for the
   pre-rendered CGI look. Must keep faction bullet colours (player blue/white, Vrell
   teal/violet, Ascendancy red/gold) readable against the background.
5. **Hazards & set pieces** – environmental rules and memorable moments.
6. **Natives** – which factions and kinds of enemies appear (generic; the actual enemy types
   live in [enemies](../enemies/README.md)).
7. **Music mood** – brief for the act music (see [audio](../audio/README.md)).

### Global rules

- Background layers never contain anything that looks like a bullet or a pickup.
- Every setting has at least one "signature" set piece that appears in no other setting.
- Hazards are introduced gently: first seen in a harmless form, then used against the player.

## Decisions

- 2026-09-30: Eight settings; fixed directory names; section template for setting documents.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../reviews/acts-1-2/README.md).
