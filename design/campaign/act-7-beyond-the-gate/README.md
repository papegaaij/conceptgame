---
title: Act 7 – Beyond the Gate
design: draft
implementation: not-started
art: none
updated: 2026-09-30
---

# Act 7 – Beyond the Gate

## Summary

Levels 43–50. The UTC fleet follows Vorne through the Tether Gate into Vrell space, where
living hive reefs drift among dead stars. Aegis Wing fights through the gate defences and
distorted space, finds the graveyard of Vrell ships killed by something else, and puts an end
to Vorne. After a boss rush of the Choir's reborn champions, the wing reaches the Choir Heart.
The ending hints at the Silence the Vrell were fleeing.

## Roster

| # | Name | Setting | Layers | Directions | Density | Recommended traits | Introduces | Notes | Design |
|---|---|---|---|---|---|---|---|---|---|
| 43 | The Tether Gate | [vrell-space](../../world/vrell-space/README.md) – gate approach | space | front | 4 | forward, shield-breaker | [Gate Warden](../../enemies/space/README.md): shielded, vulnerable only while firing | The gate's ring structure fills the deep layer | idea |
| 44 | Crossing | [vrell-space](../../world/vrell-space/README.md) – inside the gate | space | all | 4 | spread | [Rift Skater](../../enemies/space/README.md); **distorted space**: the scroll direction drifts sideways and briefly reverses | Surreal visuals; the parallax layers slide against each other. [Coilwyrm](../../enemies/air/README.md)s return, swirling with the distorted scroll | idea |
| 45 | Hive Reef | [vrell-space](../../world/vrell-space/README.md) – hive reefs | ground (reef), air | front | 5 | anti-ground, area | Spawner-heavy reef where every structure breeds | `destroy-targets`: the reef's spawning organs. [Threadcrawler](../../enemies/ground/README.md)s crawl the reef; spawning organs release [Whirl Seed](../../enemies/air/README.md) clusters | idea |
| 46 | Graveyard of the Choir | [vrell-space](../../world/vrell-space/README.md) – dead fleet | space | all | 3 | homing, piercing | [Husk](../../enemies/space/README.md): derelict Vrell ships touched by the Silence | **Breather** in tone: eerie, sparse, lots of salvage; a Silence hint. A dying [Leviathan](../../enemies/space/README.md) drifts through the dead fleet | idea |
| 47 | Seraph Gauntlet | [vrell-space](../../world/vrell-space/README.md) – Choir inner sphere | air | all | 5 | beam, homing | [Choir Seraph](../../enemies/air/README.md) elites that teleport and mirror your movement | The hardest regular waves in the game | idea |
| 48 | Vorne's End | [vrell-space](../../world/vrell-space/README.md) – Vrell-grown citadel | air, ground | front, rear | 5 | rear, spread | Mid-boss [Vorne's Chimera](../../enemies/bosses/README.md): Vorne fused with his Vrell lifeboat | The Choir has "absorbed" Vorne; pulp villain finale | idea |
| 49 | Echoes | [vrell-space](../../world/vrell-space/README.md) – Choir memory | all | all | 5 | any balanced loadout | **Boss rush**: reborn Brood Carrier, Dust Colossus and Iron Sovereign in shortened fights | Health and specials refill between bosses | idea |
| 50 | Choir Heart | [vrell-space](../../world/vrell-space/README.md) – hive core | all | all | 5 | all | Final boss [Choir Heart](../../enemies/bosses/README.md) | Ending: the Choir's last words warn of the Silence | idea |

## Design

### Synopsis

The UTC fleet pursues Vorne through the Tether Gate. On the far side lies the region the Vrell
call home: a dim cluster where their living reefs grow around dying stars. The gate is guarded,
and crossing it bends space. Beyond, the wing finds a graveyard of Vrell ships, not destroyed by
weapons but *emptied*. Vorne, now fused with the Vrell organism that saved him, makes a last
stand. The Choir throws its remembered champions at the wing, and in the hive core Aegis Wing
destroys the Choir Heart. Dying, the Choir says it only wanted to survive, and that the Silence
is coming for humanity too.

### Story beats

- L43: the fleet crosses into unknown space. Okafor stays on the fleet's flagship; the radio
  gets noisier as the wing goes deeper.
- L46: the Silence hint. Hulls hollowed out from the inside. Nobody knows what did it.
- L48: Vorne's end. Pulp dialogue at its peak, then a tragic last line.
- L49: the Choir tries to break the wing's will by re-creating what it has lost.
- L50: victory, with an open ending. The gate is sealed from the far side, and there are
  signals from deeper space.

### Settings

[Vrell space](../../world/vrell-space/README.md): gate approach (L43), gate interior (L44), hive
reefs (L45), dead fleet (L46), inner sphere (L47), citadel (L48), Choir memory (L49), hive core
(L50).

### New mechanics

- Enemies with a vulnerability window (shield opens only while firing) (L43).
- Distorted scroll direction (L44).
- Elite enemies that teleport and mirror the player (L47).
- Boss rush with refills between fights (L49).

### Factions present

Vrell (elite), Vorne's Chimera (the last of the Ascendancy), Silence-touched derelicts.

### Boss

[Choir Heart](../../enemies/bosses/README.md) (L50); mid-boss
[Vorne's Chimera](../../enemies/bosses/README.md) (L48); boss rush (L49) re-uses earlier bosses.

### Music

Act theme "Beyond the Gate" (alien, choral), graveyard ambience for L46, final boss theme,
ending theme; see [audio](../../audio/README.md).

## Implementation

- [ ] All 8 levels promoted to draft level documents and implemented.
- [ ] Scroll direction changes mid-level (L44).
- [ ] Boss-rush sequencing with shortened boss variants (L49).
- [ ] Ending sequence and credits.

## Open questions

- Is the Silence meant as a sequel hook (never explained) or explained in an epilogue?
  Proposal: never shown, only hinted.

## Decisions

- 2026-09-30: Act 7 has 8 levels, bringing the total to 50.
- 2026-09-30: Enemy variety pass: new units added to the level rows so the act passes the [variety checklist](../../enemies/README.md#variety-checklist-per-act).
