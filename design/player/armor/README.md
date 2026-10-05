---
title: Armour
design: approved
implementation: done
art: none
depends-on: [../shields, ../../systems/retry, ../../systems/difficulty]
updated: 2026-10-05
---

# Armour

## Summary

Armour is the Stormhawk's hull points. It does not regenerate during a level (except through
auto-repair or armour patches). When it reaches zero the ship is destroyed and the level is
[retried](../../systems/retry/README.md).

## Design

<!-- data: plating -->
| Plating level | Max armour | Price (first draft) | Available |
|---|---|---|---|
| Standard | 60 | starter | start |
| Composite I | 80 | 600 | act 1 |
| Composite II | 100 | 1 400 | act 2 |
| Composite III | 120 | 3 000 | act 3 |
| Composite IV | 140 | 6 000 | act 5 |
| Composite V | 160 | 11 000 | act 6 |
<!-- /data -->

- Plating draws no power and has no speed penalty. Its price is the only cost.
- **Repairs** happen between levels in the hangar. Cost per point depends on difficulty (easy:
  free, medium: 5 cr, hard: 10 cr). See [difficulty](../../systems/difficulty/README.md).
  Repairing is optional: flying a level damaged is a legitimate way to save credits.
- In-level sources: armour patches (+10) and the [auto-repair module](../systems/README.md).
- Low-armour warnings at 30 % (smoke, beeping) and 15 % (sparks, faster beep, radio line).
- **The low-armour radio line**: Okafor, grim, *"Lancer, your hull won't take much more. Fly
  careful."* (`radio` in [data.yaml](data.yaml), one line for the whole campaign). It is spoken
  once per attempt, by the first armour hit that leaves the armour at or below 15 % of its maximum
  and above zero (a hit that destroys the ship gets the failure screen's line instead). A ship
  that starts the level at or below 15 % hears it on its first armour hit; repairs (patches,
  auto-repair) do not set it off again in the same attempt; a retry, or a Retry from boss, is a new
  attempt. On the radio it is an **urgent** line ([HUD](../../ui/hud/README.md), Radio): it plays
  at once, interrupting the line on the radio, which replays after it; a warning that waits for a
  gap or goes stale is no warning.

## Implementation

- [x] Armour value, max per plating level, damage and death at 0
- [x] Hangar repair with difficulty-dependent cost, "repair all" button
- [x] Low-armour warnings on the HUD: the armour readout flashes red at 30 % and faster at 15 %
  (M4 part H, `vanguard.game.level.LowArmour`; see [HUD](../../ui/hud/README.md))
- [x] Low-armour warnings on the ship: a smoke trail behind the engines at 30 %, denser with sparks
  flying off the hull at 15 % (M4 part H, `vanguard.game.render.ShipLooks`, sprites from
  `tools/art/ship_fx.py`)
- [x] Low-armour beeps: every 1.2 s at 30 %, every 0.6 s at 15 %, 6 dB under the warnings, on the
  same `LowArmour` stages as the HUD's flashing readout (M4 part H, `vanguard.game.audio.FlightSounds`)
- [x] The radio line at 15 %, voiced, once per attempt (M4 part H: `Defences.CRITICAL_SHARE`,
  `SimEvents.Type.ARMOUR_CRITICAL`, queued urgent by the level screen; `DefencesTest`,
  `LowArmourLineTest`, `RadioTimelineTest`, `VoiceFilesTest`); for the user's ear in round 26

## Decisions

- 2026-09-30: Plating has no downside besides price; the interesting trade-off is repair
  cost vs saving credits.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../reviews/acts-1-2/README.md).
- 2026-10-01: M1 implementation: standard plating (60) in `vanguard.sim.Plating`, damage after the shield and destruction at 0 (`Defences`), which triggers the [retry](../../systems/retry/README.md) restart. Other plating levels, repairs and the low-armour warnings are later milestones.
- 2026-10-02: The plating levels moved into [data.yaml](data.yaml) (M2 data files); the table is rendered from it and `vanguard.sim.Plating` is built from it.
- 2026-10-02: M3 part B2: plating is bought in the hangar and flies (`SimSpecs.loadout`); repair
  per whole point at the difficulty's cost (easy free, medium 5, hard 10) in the hangar's repair
  panel, which opens on everything the credits pay for ("repair all") and changes by 1 or 10
  points. A plating swap keeps the damage: the missing points stay missing (at least 1 point is
  left); the document gave no rule, see the hangar's open questions.
- 2026-10-02: Plating swap (user decision): the missing armour points carry over to the new
  plating (Standard 41/60 → Composite I 61/80), at least 1 point left, as built in M3 part B2;
  keeping the points or the share were the alternatives. The rule is in the
  [hangar](../../ui/hangar/README.md#transactions).
- 2026-10-05: M4 part H: the HUD's low-armour flash at or below 30 % and 15 % of the max armour
  (`LowArmour.LOW` / `CRITICAL`, the same thresholds as the beeps), one flash per beep period (1.2 s,
  0.6 s); see the [HUD](../../ui/hud/README.md#decisions).
- 2026-10-05: M4 part H: the low-armour radio line (a default of the part's gap list): one Okafor
  line, grim, *"Lancer, your hull won't take much more. Fly careful."*, in this part's
  [data.yaml](data.yaml) (`radio`) rather than in every level's radio cues, so it is one line for
  the whole campaign. The simulation sets it off (`SimEvents.Type.ARMOUR_CRITICAL` from
  `Defences`, once per attempt, by the first armour hit to at or below `Defences.CRITICAL_SHARE` =
  15 %, the threshold `LowArmour` now takes from it; the flag enters the state hash only once set,
  so Level 01's replay hash is unchanged); the level screen queues it as an **urgent** line, like
  the Airstrike's call: an event line could be dropped as stale and a closing line waits for a gap,
  while the warning matters only now. Voiced by `tools/art/voice.py`
  (`assets/voice/okafor/14b14dfe36a3.ogg`, 4.5 s of speech on the key's seed), for the user's ear
  in round 26.
