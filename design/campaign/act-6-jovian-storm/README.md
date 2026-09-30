---
title: Act 6 – Jovian Storm
design: draft
implementation: not-started
art: none
updated: 2026-09-30
---

# Act 6 – Jovian Storm

## Summary

Levels 36–42. The UTC counter-offensive drives into the Ascendancy heartland: the gilded cloud
cities floating in Jupiter's atmosphere, the refineries of Io, and Helix Dynamics headquarters
on Callisto. Hybrid Ascendancy/Vrell biotech machines appear: they regenerate, mirror-shielded
interceptors reflect shots, and kamikaze hybrids split apart on death. The act ends in orbit
against Vorne's flagship **Ascendant**. Vorne escapes into the Tether Gate.

## Roster

| # | Name | Setting | Layers | Directions | Density | Recommended traits | Introduces | Notes | Design |
|---|---|---|---|---|---|---|---|---|---|
| 36 | Cloud Divers | [jovian](../../world/jovian/README.md) – upper atmosphere | air, low-air, high-air | front | 4 | forward, homing | [Chimera](../../enemies/air/README.md), a hybrid fighter that regenerates unless killed quickly; foreground cloud banks hide enemies | Act opener; storm bands in the deep layer | idea |
| 37 | Gilded Cage | [jovian](../../world/jovian/README.md) – cloud city *Aurelia* | ground (platforms), air | front, sides | 4 | anti-ground, spread, shield-breaker | [Graft Turret](../../enemies/ground/README.md) (regrows), [Mirror Interceptor](../../enemies/air/README.md) (reflects non-beam shots) | Black-and-gold Ascendancy architecture, Vrell growth creeping over it | idea |
| 38 | Lightning Belt | [jovian](../../world/jovian/README.md) – storm layer | air | all | 5 | homing, beam | [Harrow](../../enemies/air/README.md) kamikaze hybrids that burst into a ring on death; lightning strikes | Lightning is telegraphed by a flash, then strikes a vertical line. [Buzzsaw Drone](../../enemies/air/README.md)s return, ricocheting between storm cells | idea |
| 39 | Io Flyby | [jovian](../../world/jovian/README.md) – Io refineries | ground, air | front | 4 | anti-ground, piercing | Volcanic eruptions (ground-to-air hazard); refinery targets | `destroy-targets`: cut the Ascendancy's fuel supply. [Strider](../../enemies/ground/README.md)s return, guarding the refineries | idea |
| 40 | Honour Guard | [jovian](../../world/jovian/README.md) – Callisto approach | air | front, sides, rear | 5 | spread, rear | Mid-boss [Honour Guard](../../enemies/bosses/README.md): three ace pilots who attack from three directions | Duel: the aces taunt over the radio | idea |
| 41 | Callisto Fortress | [jovian](../../world/jovian/README.md) – Helix HQ, Callisto | ground, air | front | 5 | anti-ground, area | [Shield Pylon](../../enemies/ground/README.md) domes protecting fortress sections; [Halo Platform](../../enemies/ground/README.md) rotating turret rings as fortress centrepieces | Break the pylons to open each section of the fortress. [Warden Tank](../../enemies/ground/README.md)s return in black and gold | idea |
| 42 | Ascendant | [jovian](../../world/jovian/README.md) – Callisto orbit | air, space | all | 5 | piercing, homing | Boss [Ascendant](../../enemies/bosses/README.md), Vorne's flagship | Vorne escapes in a Vrell-grown lifeboat toward the gate | idea |

## Design

### Synopsis

With the Belt retaken, the UTC fleet jumps to Jupiter. Aegis Wing dives into the atmosphere to
break the air defences of the cloud city **Aurelia**, the Ascendancy's gilded capital, where Vrell
growth now creeps over the gold. They fly through Jupiter's storm layers, cut the refinery
lines on Io, and duel Vorne's three honour-guard aces. Then they break into Helix Dynamics'
fortress on Callisto. Vorne makes his last stand in orbit aboard his flagship *Ascendant*,
which is half ship and half Vrell organism. When it breaks apart, a Vrell lifeboat carries him
toward the Tether Gate.

### Story beats

- L36: the Ascendancy's biotech has its own cost. Hybrid pilots are no longer entirely human.
- L37: **the Vrell turn.** Vrell growth is consuming Aurelia, and its civilians have been partly
  "grafted". Varga is shaken. From here the Vrell attack Ascendancy targets too: a three-way war
  (see [story](../../story/README.md)).
- L40: the Honour Guard aces are the pulp highlight, with a cocky ace rivalry against Rook.
- L41: in Helix HQ, Varga finds Vorne's deal with the Vrell. They are *fleeing* something, and
  offered biotech for safe passage to the inner system. This is the first mention of "the
  Silence".
- L42: Vorne escapes toward the gate in a Vrell lifeboat, still believing the Choir will honour
  the deal. Okafor: "*Then we follow him.*"

### Settings

[Jovian](../../world/jovian/README.md): upper atmosphere (L36), cloud city Aurelia (L37), storm
layer (L38), Io refineries (L39), Callisto (L40–41), Callisto orbit (L42).

### New mechanics

- Regenerating enemies: damage has to be applied in bursts (L36).
- Reflecting shields: non-beam shots bounce back (L37).
- Enemies that burst into bullet rings on death (L38).
- Telegraphed environmental strikes (L38–39).
- Shield pylons that make sections invulnerable (L41).

### Factions present

Jovian Ascendancy, Ascendancy/Vrell hybrids, Vrell.

### Boss

[Ascendant](../../enemies/bosses/README.md) (L42); mid-boss
[Honour Guard](../../enemies/bosses/README.md) (L40).

### Music

Act theme "Jovian Storm", Honour Guard duel theme, boss theme; see [audio](../../audio/README.md).

## Implementation

- [ ] All 7 levels promoted to draft level documents and implemented.
- [ ] Reflect mechanic for mirror shields (L37) and burst-on-death ring (L38).
- [ ] Foreground cloud occlusion that hides enemies without hiding bullets (L36).

## Open questions

- Must enemy bullets always stay visible above foreground clouds? Proposal: yes. Clouds may hide
  enemy bodies but never bullets (readability rule in [enemies](../../enemies/README.md)).

## Decisions

- 2026-09-30: Vorne survives Act 6 and is dealt with in Act 7 (L48).
- 2026-09-30: Enemy variety pass: new units added to the level rows so the act passes the [variety checklist](../../enemies/README.md#variety-checklist-per-act).
