---
title: Needler
design: approved
implementation: in-progress
art: final
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-02
---

# Needler

## Summary

The basic Vrell gunner: an ivory crab-like flyer (design language B) that hovers or swoops and fires slow aimed thorns. The first enemy that shoots back.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis.

<!-- data: stat-block -->
| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `air` |
| Size tier | `small` |
| Size | 36×36 px, hitbox 26×24 |
| Parts | single |
| Orientation | `fixed` |
| HP | 4 (easy 3 / hard 5, from the global multipliers) |
| Armour / shield | none |
| Speed | 120 px/s entering, 0 while hovering |
| Movement | `swoop` in, `hover` 2–4 s at y = 80–220 px, then exit down or to the side; in `circle` formations `orbit` a point (radius 90 px, 60°/s) |
| Attack | `aimed` thorn (standard orb) every 2.5 s, 150 px/s, damage `small` = 4; first shot 0.8 s after it stops |
| Formations | V-wing (5), line abreast, pincer, circle (8) |
| Weak points | glowing violet eye cluster (×1.5) |
| Effective traits | `forward`, `spread` |
| Credits | 12 (score 120 × chain) |
| Death | `small` burst: ivory shards, violet flash; every 4th Needler kill in a level drops a shield cell |
| First level / used in | L01; recurring gunner through Acts 1–2 and spawned by the Brood Carrier |
| Difficulty hooks | hard: selected Needlers in `circle` formations lead the target |
<!-- /data -->

### Behaviour

- The first shot is delayed after the Needler stops so a new player can kill it before it fires.
- Thorns fly at standard speed, so they are drawn as the Vrell **standard orb** (magenta) of the
  bullet set; the yellow needle is reserved for the fast class (bullet readability rules).

### Concept art

Chosen concept: [needler-r04-a.png](../concept/needler-r04-a.png) (listed in the [air](../README.md#concept-art) Concept art table).

## Concept art

Production art for concept round 12 (the Level 01 batch; part P2 opens the round), review files built from the final frames in `assets/` by `tools/art/vrell_air.py` (`--review` rebuilds only them); prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/needler-final-r12-a.png](concept/needler-final-r12-a.png) | Final sprites: the 6-frame claw snap (36×36, 32 colours; opens over three frames, snaps shut in one), at 5× and 1× | chosen |
| [concept/needler-final-r12-a.gif](concept/needler-final-r12-a.gif) | The cycle at 10 fps, three units at their own phase as in a formation | chosen |

## Implementation

- [x] Hover-and-fire behaviour with the 0.8 s first-shot delay
- [x] Circle formation orbiting and breaking off
- [x] Thorn bullet: standard orb, 150 px/s
- [x] Stat block values loaded from data; global difficulty multipliers applied
- [ ] Death effect, bounty and score per this spec

## Decisions

- 2026-10-01: Promoted from the air roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-02: The stat block moved into [data.yaml](data.yaml) and is rendered from it (M2 data files), with the hover, orbit and attack numbers as fields for the Level 01 work.
- 2026-10-02: Drop rule (user decision): every 4th Needler kill in a level drops a shield cell (a
  deterministic counter, `drops` in [data.yaml](data.yaml)); the Death row is rendered from it.
- 2026-10-02: M2 implementation (`vanguard.sim.Enemy`, `EnemySpec` and `EnemyGun` built by
  `vanguard.content.SimSpecs`): swoop in along an entry path, hover 2–4 s (random per unit, from
  the level seed) 80–220 px below the top, first thorn 0.8 s after it stops, then every 2.5 s
  (easy ≈3.6 s, hard ≈1.9 s by the fire-rate lever), 150 px/s (easy 120, hard 172.5), then leave
  down and out to its side. In a circle it orbits (90 px, 60°/s) and breaks toward the ship; on
  hard every second Needler of a circle leads the target. Bullets need a 72 px gap to the ship and
  fit the bullet budget. A rammed Needler is destroyed (small unit). Not done yet: the weak point
  (×1.5) has no hit box position in the design, so hits do normal damage; the death effect is the
  generic 40 px fire explosion (not ivory shards with a violet flash), so that item stays open.
  Placeholders: the 3 claw-snap frames of [needler-r04-a](../concept/needler-r04-a.png) and the
  Vrell yellow needle of [enemy-bullets-r09-a](../../concept/enemy-bullets-r09-a.png).
- 2026-10-02: The thorn uses the Vrell standard orb look (user decision), as the bullet set gives
  the yellow needle to the fast class (190–260 px/s) and the thorn flies at 150 px/s. Placeholder:
  the first orb frame of [enemy-bullets-r09-a](../../concept/enemy-bullets-r09-a.png), cut by
  `:pipeline:importPlaceholders` (replaces the needle).
- 2026-10-02: Production art (Level 01 batch, `tools/art/vrell_air.py`): the 6-frame claw snap (36×36, 32 colours), rendered at 8× from the chosen round-04 model with one palette for the cycle (the placeholder had 3 frames, which snapped from the last back to the first). `orientation: fixed` in the stat block, so no heading set; the production plan's "angle sets" for Level 01's enemies are therefore not rendered (open point for the user). Review files proposed for round 12.
- 2026-10-02: Orientation stays `fixed` (user decision, Level 01 batch part P3); the claw snap got
  a wider swing (opening over three frames, snapping shut in one) so it reads in play.
- 2026-10-02: Concept round 12 closed (user decision): the 6-frame claw-snap production sprites approved as **final**, `art: final`.
