---
title: Naval enemies
design: draft
implementation: not-started
art: chosen
updated: 2026-10-01
---

# Naval enemies

## Summary

Enemies on water: surface units (treated as `ground` layer targets on the water surface) and
submerged units on the `sub` layer. They appear on Earth's oceans and arctic in Act 2, and are
the core of the underwater Act 4 on Europa, where the `sub` layer becomes the play plane.

## Contents

Promoted to their own documents for the Acts 1–2 wrap-up; the remaining units stay in the roster below.

| Part | Summary | Design | Impl | Art |
|---|---|---|---|---|
| [driftjelly](driftjelly/README.md) | Jellyfish mine on or below the surface, proximity ring (L11) | draft | not-started | chosen |
| [reef-spitter](reef-spitter/README.md) | Barnacle gun on a kelp raft, 3-way fans (L11) | draft | not-started | chosen |
| [skimmer](skimmer/README.md) | Flying-fish skiff weaving between floes from every edge (L13) | draft | not-started | chosen |

## Roster

| Name | Faction | Layer | Tier | Role/ability | Formations | First level | Design |
|---|---|---|---|---|---|---|---|
| Eel Swarm | Vrell | sub | large | A long snake of eel segments; each segment has to be destroyed, and the head fires `aimed` shots. | snake | 23 | idea |
| Abyss Ray | Vrell | sub | large | A large manta rising from the depths (it grows from a shadow); fires a wide `fan` wave, then glides away. | carrier + escorts | 24 | idea |
| Siren | Vrell | sub | medium | Sends out sonar `ring`s that do no damage but **slow** the player for 2 s. Dangerous in combination with others. | turret nest (in walls) | 25 | idea |
| Glow Angler | Vrell | sub | small | In darkness its lure looks like a pickup. When the player is close it lunges (`dive`) and fires a `ring`. | swarm (scattered) | 26 | idea |
| Depth Hunter | Unmarked (Helix Dynamics) | sub | medium | A human-built sub drone firing slow shootable `homing` torpedoes up the screen. **Hint 3.** | column | 27 | idea |
| Spiral Nautilus | Vrell | sub | medium | *(roster fork addition)* A coiled-shell cephalopod that rolls in its shell (`spin`, `radial` shell with a 16-angle tentacle crown) along `spiral-in` paths around the player, firing a `spiral` from its tentacles when it uncoils. The shell is `armoured`; shoot it while uncoiled. | circle (pairs orbiting in opposite directions) | 25 | idea |

## Design

- Above water (Act 2), `sub` units are shadows under the waves: hittable only by `anti-sub`,
  per the [layer rules](../README.md#layer-rules). Surfaced units are ground targets.
- Underwater (Act 4), the `sub` layer is the play plane: all weapons hit, but weapons without
  `anti-sub` do 50% damage. Enemy bullets and movement are slowed by the water. All numbers:
  [under water rules](../../world/europa/README.md#under-water-rules).

## Concept art

Concept [round 06](../../concept-rounds/round-06/README.md) — generator `tools/concept/enemies_r06.py`. Prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/driftjelly-r06-a.png](concept/driftjelly-r06-a.png) | Driftjelly — jellyfish mine (olive bell, lime veins), surface and submerged, pulses a ring of shots (sheet) | superseded by r07 (waterline) |
| [concept/driftjelly-r06-a.gif](concept/driftjelly-r06-a.gif) | Driftjelly — jellyfish mine (olive bell, lime veins), surface and submerged, pulses a ring of shots (motion) | superseded by r07 (waterline) |
| [concept/reef-spitter-r06-a.png](concept/reef-spitter-r06-a.png) | Reef Spitter — barnacle gun at 32 headings on a bobbing kelp raft, 3-way fans (sheet) | chosen |
| [concept/reef-spitter-r06-a.gif](concept/reef-spitter-r06-a.gif) | Reef Spitter — barnacle gun at 32 headings on a bobbing kelp raft, 3-way fans (motion) | chosen |
| [concept/skimmer-r06-a.png](concept/skimmer-r06-a.png) | Skimmer — flying-fish skiff (rust, fan fins) weaving through ice floes with foam wakes (sheet) | chosen |
| [concept/skimmer-r06-a.gif](concept/skimmer-r06-a.gif) | Skimmer — flying-fish skiff (rust, fan fins) weaving through ice floes with foam wakes (motion) | chosen |
| [concept/spiral-nautilus-r06-a.png](concept/spiral-nautilus-r06-a.png) | Spiral Nautilus — rolling striped shell with a tentacle crown, orbiting pairs under Europa's ice (sheet) | chosen |
| [concept/spiral-nautilus-r06-a.gif](concept/spiral-nautilus-r06-a.gif) | Spiral Nautilus — rolling striped shell with a tentacle crown, orbiting pairs under Europa's ice (motion) | chosen |

Concept [round 07](../../concept-rounds/round-07/README.md) — real waterline (art-direction Water rules); generator `tools/concept/enemies_r07.py`.

| File | What | Status |
|---|---|---|
| [concept/driftjelly-r07-a.png](concept/driftjelly-r07-a.png) | Driftjelly r07 — bell cut at the water plane, lower bell and tentacles visible under water, broken foam collar, ripple trains; trigger radius now a labelled diagram (sheet) | chosen |
| [concept/driftjelly-r07-a.gif](concept/driftjelly-r07-a.gif) | Driftjelly r07 — surfaced and submerged jellies drifting, ripples left behind as they pulse | chosen |

## Implementation

- [ ] Each enemy promoted to its own directory with a stat block before it is implemented.
- [ ] Surfacing/submerging transition visuals (shadow ⇄ sprite).

## Open questions

- Does the Glow Angler's fake pickup risk frustrating players? Proposal: its lure glows a
  slightly different colour than real pickups, and Varga warns about it on its first appearance.

## Decisions

- 2026-09-30: Roster of 8 naval enemies drafted.
- 2026-09-30: Converted to the 960×540 baseline (was 640×360).
- 2026-09-30: Enemy variety pass: size tier column added to every row; new units Spiral Nautilus (roster fork addition).
- 2026-10-01: Concept round 06: Reef Spitter, Skimmer and Spiral Nautilus chosen; Driftjelly chosen but its waterline ring must become real foam/ripples (round 07).
- 2026-10-01: Concept round 07: Driftjelly waterline fixed; r07 chosen.
- 2026-10-01: Acts 1–2 units promoted to full specs: Driftjelly, Reef Spitter, Skimmer.
- 2026-10-01: Above-water `sub` targets are hit by `anti-sub` only (`area` removed), matching the layer rules in the enemies README.
