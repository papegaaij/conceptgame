---
title: Concept round 05 — enemy variety: sizes, multi-part, walkers, spinners, movement
design: approved
implementation: n/a
art: chosen
updated: 2026-09-30
---

# Concept round 05 — enemy variety

## Summary

User feedback after round 04: enemies were too similar in size and shape — all insect-like
flyers with a fixed front and back. This round adds new archetypes on top of the roster (the
rules and roster changes are in [enemies](../../enemies/README.md): size tiers, multi-part
rules, 16/32-angle orientation, extended movement vocabulary and a per-act variety checklist).
Open [index.html](index.html) in a browser — **watch the GIFs**, they show the movement
(regenerate with `python3 tools/concept/board.py 05`). All units use the adopted role colours.

## Choices

| # | Choice | Variants | Part | Outcome |
|---|---|---|---|---|
| 1 | Coilwyrm — segmented serpent | Head + 12 segments following a looping swirl, 16 headings per part | [enemies/air](../../enemies/air/README.md) | chosen |
| 2 | Leviathan — huge whale-like set piece | Articulated tail and fins with a travelling wave, dorsal turrets, ~480 px | [enemies/space](../../enemies/space/README.md) | chosen |
| 3 | Scuttler — crab walker | Tripod gait, 16 headings × 6 walk phases | [enemies/ground](../../enemies/ground/README.md) | chosen |
| 4 | Whirl Seed — tiny spinner | Spiralling, bouncing clusters | [enemies/air](../../enemies/air/README.md) | chosen |
| 5 | Mote Swarm — flock with rear attack | Boids swirl, exits and returns from behind with an edge warning | [enemies/air](../../enemies/air/README.md) | chosen |
| 6 | Warden Tank | Hull and turret turning independently | [enemies/ground](../../enemies/ground/README.md) | chosen — kept for the Ascendancy (keeps its unmarked L19 hint) |
| 7 | Strider — walker mech | Walk cycle, twisting torso | [enemies/ground](../../enemies/ground/README.md) | chosen — kept for the Ascendancy |
| 8 | Buzzsaw Drone — spinner | Ricochets with an after-image trail | [enemies/air](../../enemies/air/README.md) | chosen |
| 9 | Rail Serpent — drone train | Head car + 8 cars on a winding route | [enemies/air](../../enemies/air/README.md) | chosen |
| 10 | Size range and the new rules | Size lineup; size tiers, multi-part, orientation, movement vocabulary, variety checklist in the enemies doc; 13 new roster units incl. Threadcrawler, Halo Platform, Dust Devil and Spiral Nautilus (no concept art yet) | [enemies](../../enemies/README.md) | accepted |
| 11 | Ravager — animal-like pack hunter | Hound/raptor-like four-legged beast, gallop cycle, pounces to low-air; first at L09 | [enemies/ground](../../enemies/ground/README.md) | chosen |
| 12 | Shellback — armoured beast | Tortoise/armadillo-like, spore mortar, curls into a ball and rolls; first at L17 | [enemies/ground](../../enemies/ground/README.md) | chosen |

## Decisions

- 2026-09-30: Round opened.
- 2026-10-01: Choices 1–10 answered: all archetypes liked; Warden Tank and Strider are too mechanical for the Vrell and stay Ascendancy-only. Follow-up: two animal-like Vrell ground walkers (Ravager, Shellback) added to this round.
- 2026-10-01: Ravager and Shellback chosen. Round closed.
