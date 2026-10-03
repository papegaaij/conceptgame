---
title: Concept round 17 — M4 part D, Level 04
design: approved
implementation: n/a
art: chosen
updated: 2026-10-03
---

# Concept round 17 — M4 part D, Level 04

## Summary

The production-art round of M4 part D ([roadmap](../../tech/roadmap/README.md#m4-parts)): Level
04's civilian crawler, the Airstrike's CDF bomber, the Luna backdrop and the hangar intel
portraits of the Brood Pod and the Scuttler, rendered as **final** art by the generators in
[tools/art/](../../../tools/art/README.md) into `assets/`; concepts for the prospector's dugout and the
CDF supply drop; game captures of the level; and the numbers the agents chose to close part D's
doc gaps. Open [index.html](index.html) in a browser (regenerate with
`python3 tools/concept/board.py 17`). Every art choice is *approve as final* or *redo* (say what
to change); the concepts are *pick a or b*; only an approved part gets `art: final`. Level 04 is
playable with `./gradlew :desktop:run --args="--level 4 --special airstrike:2"` (the `--special`
option fits the Airstrike for testing; in a real campaign the free charge is given at the hangar
before Level 04). Choices 9–10 were added on 2026-10-03 after the user's verdict on 1–8: the bridge
and gate redo, and the production art of the chosen dugout and supply drop.

## Choices

| # | Part | What is proposed | Review files | Weak spots (Claude) | Outcome |
|---|---|---|---|---|---|
| 1 | [Civilian crawler](../../allies/README.md) | production art of the chosen r16 c bus with its cargo sled, 40×72 (`tools/art/civilian_crawler.py`) | `civilian-crawler-final-r17-a` (png, gif) | the beacon glow is not in the sprites; the tread motion is subtle; the HUD pip is hand-made | approved — final |
| 2 | [Airstrike CDF bomber](../../player/specials/README.md) | production art of the Hammer bombers crossing with their shadows (`tools/art/airstrike_bomber.py`) | `airstrike-bomber-final-r17-a` (png, gif) | the flames are baked in; the shadow carries its own 50 % opacity | approved — final |
| 3 | [Level 04 Luna backdrop](../../campaign/act-1-first-contact/level-04-tranquility-run/README.md) | every tile set and piece of the Luna road run, from the start to the terminal gate | `backdrop-final-r17-a` | the gate's landing lights are small; the pod husks read as open bowls; the sled races about 550 px/s as an animation; the tile seams depend on the section times; the crawlers draw over the terminal gate instead of entering it | approved — final, except the bridge: the crawlers pass over the arches that also hold the turrets; redone in item 9 (`backdrop-final-r17-a` has since been regenerated in place with the split bridge and gate, so it is no longer the image reviewed here) |
| 4 | [Intel portraits](../../ui/hangar/README.md) | the 30×30 sensor portraits of the Brood Pod and the Scuttler (`tools/art/intel.py`) | `intel-final-r17-a` | the Brood Pod is dark | approved — final |
| 5 | [Prospector's dugout](../../campaign/act-1-first-contact/level-04-tranquility-run/README.md) | **concept**, 48×32, hardened: (a) or (b), see the level's Concept art | `dugout-r17-a`, `-b` | pick a variant | a chosen (b moved to `concept/rejected/`); its production art is item 10 |
| 6 | [CDF supply drop](../../campaign/act-1-first-contact/level-04-tranquility-run/README.md) | **concept**, 32×24, 3 HP: (a) or (b), see the level's Concept art | `supply-drop-r17-a`, `-b` | pick a variant | a chosen (b moved to `concept/rejected/`); its production art is item 10 |
| 7 | [Level 04 in the game](../../campaign/act-1-first-contact/level-04-tranquility-run/README.md) | game captures with `--level 4` | `level-04-capture-final-r17-a` | the Scuttler's death plays the `medium` explosion (its organic burst is `later:`); the dugout and the supply drop use placeholder container frames until their concepts are chosen | approved |
| 8 | [Part D numbers chosen by the agents](#part-d-numbers) | accept or change the numbers listed under [Part D numbers](#part-d-numbers) | — | hard may need a look with a realistic loadout (the autopilot gets only 2 of 5 crawlers home) | accepted as listed |
| 9 | [Level 04 bridge and gate](../../campaign/act-1-first-contact/level-04-tranquility-run/README.md) | **redo of 3's bridge**: the crawlers pass under the road bridge's arches, which carry the Spine Turrets, and drive in under the terminal gate's roof, through a new `overhead` ground-piece pass | `level-04-capture-final-r17-b` (stills of the supply drop, the bridge crossing, the dugout and the gate), the regenerated `backdrop-final-r17-a`; after the user's feedback that the hangar was far too small, `level-04-capture-final-r17-c` (the hangar, 5× larger, 380×300, the crawlers driving in) | a Scuttler walking under an arch or the gate is drawn on top of it; crawlers that have entered the gate are still live in the sim (just not drawn) | approved — final (the bridge and the enlarged hangar), with the captures |
| 10 | [Dugout and supply drop](../../campaign/act-1-first-contact/level-04-tranquility-run/README.md) | production art of the chosen a models: intact, damaged and wrecked frames plus break-apart frames (`tools/art/l04_targets.py`) | `l04-targets-final-r17-b` (png, gif) | the supply drop's wrecked frame has a dark block clipped at its top edge; the break and wreck frames have not been seen in the game yet | approved — final |

## Part D numbers

Chosen by the agents to close part D's doc gaps (choice 8); the details live in the parts' READMEs
and Level 04's `data.yaml`.

- **Road**: 56 px wide, at most 30° of bend, x 72–408.
- **Crawler**: hitbox 32×64; the column's centres at y = 150 / 234 / 318 / 402 / 486, the lower
  79 % (five 72 px crawlers 84 px apart do not fit in 70 %).
- **Brood Pod**: a 4 s sine swing; its timer runs from crossing the top edge; the Skitters are
  released at 160 px/s; a self-burst pays 8, does not chain and counts as an escape; escorts break
  off 0.5 s apart.
- **HUD**: two-objective wells at 384–430 and 436–482.
- **Text**: the prompt `SPECIAL · CALL HAMMER`; the intel line `ESCORT 5 CRAWLERS`; the briefing
  lines "ESCORT THE 5 CRAWLERS TO THE END" and "BONUS: KILL EVERY BROOD POD BEFORE IT BURSTS".
- **Failed escort**: no explosion or slow motion, the ship cannot be hurt for 3 s, then the failed
  screen with the level's own line.
- **Radio**: a line interrupted by an URGENT line replays from its start; retimes: Crawler One
  t=4 → 11, Rook t=30 → 34.5, Okafor's Hammer line at about t=60 (only with the special fitted),
  the first pod at t≈22.
- **Ground**: the Scuttler paths and turret positions are authored in Level 04's `data.yaml`; the
  dugout at t≈125 (20 HP, hardened), the supply drop at t≈52.5; the secret crate pays 100.
- **Budget**: 1,225 credits at medium.
- **Autopilot** (Pulse Cannon only): 5/5 crawlers home on easy and medium, only 2/5 on hard, so
  hard may need a look with a realistic loadout.

## Notes

- Review file names: `<subject>-final-r17-a.png/.gif` (production art) and `<subject>-r17-a/b.png`
  (concepts) in each part's `concept/`; their `prompts.md` entries say how they are made. Nothing
  in `assets/` is hand-edited.

## Decisions

- 2026-10-03: Opened with M4 part D.
- 2026-10-03: User verdict on items 1–8; 1, 2 and 4 (the civilian crawler, the Airstrike's CDF bomber, the intel portraits) approved as final; 3, the Luna backdrop, approved except the bridge (the crawlers pass over the arches that also hold the turrets); 5 and 6, dugout a and supply drop a chosen (the b variants moved to `concept/rejected/`); 7, the captures, and 8, the part D numbers, accepted as they are. The round stays open: the bridge redo and the targets' production art added as items 9–10.
- 2026-10-03: Closed (user decision): 9, the bridge and the terminal hangar, approved as final after the hangar was enlarged ("that's much better"; the captures `level-04-capture-final-r17-b` and `-c` and the regenerated `backdrop-final-r17-a`), so the Luna backdrop is final; 10, the production art of dugout a and supply drop a (`l04-targets-final-r17-b`), approved as final. With it M4 part D's art is final.
