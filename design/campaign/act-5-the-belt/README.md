---
title: Act 5 – The Belt
design: draft
implementation: not-started
art: none
updated: 2026-09-30
---

# Act 5 – The Belt

## Summary

Levels 29–35. With the betrayal in the open, the Jovian Ascendancy seizes the asteroid belt and
its mining stations to cut Earth off from the outer system. For the first time Aegis Wing
fights human-built war machines openly: fast Talons, missile boats, minelayers and fortified
stations. The act introduces generator drain and minefields, and has a salvage breather before
the Iron Sovereign, the Ascendancy battle station anchored at Ceres.

## Roster

| # | Name | Setting | Layers | Directions | Density | Recommended traits | Introduces | Notes | Design |
|---|---|---|---|---|---|---|---|---|---|
| 29 | Betrayal at Ceres | [belt](../../world/belt/README.md) – Ceres Hub | air, ground (station hull) | front, sides | 4 | spread, homing | Ascendancy openly: [Talon](../../enemies/air/README.md), [SAM Nest](../../enemies/ground/README.md); [Asteroid Mite](../../enemies/space/README.md) | Ceres garrison mutinies; UTC and Ascendancy ships fight all around | idea |
| 30 | Minefield | [belt](../../world/belt/README.md) – shipping lane | space | front, rear | 3 | rear, piercing | [Minelayer](../../enemies/space/README.md) (mine lines behind it), [Gilded Gunship](../../enemies/air/README.md); destructible drifting asteroids | Minelayers pass from behind and seed the lane ahead | idea |
| 31 | Mining Rig Escort | [belt](../../world/belt/README.md) – open belt | space, ground (rig hull) | all | 4 | spread, side | [Shard Drone](../../enemies/space/README.md) groups linked by energy beams; [Hornet](../../enemies/air/README.md) missile boats | `escort`: a giant mining tug crawls up the play field | idea |
| 32 | Deep Core | [belt](../../world/belt/README.md) – inside a hollowed asteroid | ground, air | front | 4 | anti-ground, forward | Tunnel walls; [Sentinel Tower](../../enemies/ground/README.md) laser sweeps | Claustrophobic tunnels with branching routes | idea |
| 33 | Leech Field | [belt](../../world/belt/README.md) – Vrell-infested rock field | space | rear, sides | 4 | rear, beam | [Void Leech](../../enemies/space/README.md) (drains the generator); [Rail Bunker](../../enemies/ground/README.md) | Energy management level: a heavy loadout suffers when leeches drain power | idea |
| 34 | Salvage Run | [belt](../../world/belt/README.md) – ship graveyard | space, ground (derelict hulls) | front | 2 | anti-ground, spread | [Crawler Tank](../../enemies/ground/README.md) on derelict hulls; many credit caches and secrets | **Breather** before the boss | idea |
| 35 | Iron Sovereign | [belt](../../world/belt/README.md) – Ceres orbit | space, ground | all | 5 | piercing, anti-ground | Boss [Iron Sovereign](../../enemies/bosses/README.md), a rotating battle station | The station's rings rotate and bring new weapon batteries round to face the player | idea |

## Design

### Synopsis

Vorne's broadcast splits the solar system. **Ceres Hub**, where the Belt mining consortium
has its headquarters, declares for the Ascendancy, and half its garrison turns on the UTC.
Aegis Wing escapes the mutiny, then clears the mined shipping lanes and escorts a mining tug
carrying loyalist refugees. They raid an Ascendancy base built inside a hollowed asteroid and
fight through a rock field infested with Vrell leeches. After salvaging parts in a ship
graveyard, the wing attacks the **Iron Sovereign**, the battle station holding the Belt.

### Story beats

- L29: the reveal plays out. The act briefing is Vorne's broadcast. Former comrades are on the
  other side. During the mutiny the wing frees Rook, who was captured by Helix subs at Europa
  and held at Ceres Hub; he knew some of the Ceres pilots. He flies as wingman again from L30.
- L31: the refugees tell how Helix Dynamics bought loyalty with Vrell biotech that cured
  diseases.
- L33: the Vrell and the Ascendancy cooperate, but uneasily. The leeches drain Ascendancy
  ships too.
- L35: the Sovereign falls. Captured data points to Vorne's headquarters on Callisto.

### Settings

[Belt](../../world/belt/README.md): Ceres Hub (L29, L35), shipping lanes (L30), open belt
(L31), hollowed asteroid (L32), rock field (L33), ship graveyard (L34).

### New mechanics

- Human-built enemies with shields and missile volleys (L29–31).
- Mine lines laid behind the player (L30).
- Linked enemy groups whose beams act as barriers (L31).
- Tunnel walls with branching routes (L32).
- Generator drain (L33).

### Factions present

Jovian Ascendancy (openly), Vrell.

### Boss

[Iron Sovereign](../../enemies/bosses/README.md) (L35).

### Music

Act theme "The Belt" (colder, more industrial: the enemy is human now), boss theme; see
[audio](../../audio/README.md).

## Implementation

- [ ] All 7 levels promoted to draft level documents and implemented.
- [ ] Generator-drain effect on the player's power budget (L33); see [player](../../player/README.md).
- [ ] Rotating multi-part boss (L35).

## Open questions

- Should the player be able to choose to spare the mutineer pilots (a small story choice), or
  does the campaign stay strictly linear in its story too?

## Decisions

- 2026-09-30: The Belt is the first act where the Ascendancy fights openly.
- 2026-09-30: Rook is freed at Ceres Hub during L29 and is available again from L30.
