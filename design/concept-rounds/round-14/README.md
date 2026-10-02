---
title: Concept round 14 — M4 part A, the Act 1 arsenal
design: review
implementation: n/a
art: proposed
updated: 2026-10-02
---

# Concept round 14 — M4 part A, the Act 1 arsenal

## Summary

The production-art round of M4 part A ([roadmap](../../tech/roadmap/README.md#m4-parts)): the
effects of the seven Act 1 weapons beyond the Pulse Cannon, rendered as **final** art by
`tools/art/weapon_fx.py` into `assets/` from the chosen round-08 projectile families, and the HUD's
right panel with the arsenal. Open [index.html](index.html) in a browser (regenerate with
`python3 tools/concept/board.py 14`). Every choice is *approve as final* or *redo* (with what to
change); only an approved part gets `art: final`. To see them in play:
`./gradlew :desktop:run --args="--loadout front=scatter-vulcan:5,left=bomb-rack:3,right=micro-missile-pod:3,rear=side-splitter:3"`.

## Choices

| # | Part | What is proposed | Review files | Weak spots (Claude) | Outcome |
|---|---|---|---|---|---|
| 1 | [Weapon effects](../../player/weapons/README.md) | the Scatter Vulcan's and Autocannon's tracers and the Side Splitter's bolts at every angle their patterns use, the Lance per level (thicker at L3 and L5), the Micro-missile with its plume at 32 headings, the bomb and the shell (drawn shrinking as it falls, growing on its arc), the Mortar's cyan landing reticle, the ballistic and launcher muzzle flashes, the ballistic and explosive impacts; bomb and shell bursts use the small explosion | `weapons-final-r14-a` (png, gif), `weapons-capture-final-r14-a` | the side bolts and the lance levels in pairs come out pixel-identical (the packer stores them once); the bomb and the shell are 12 and 10 px and read mostly as dark dots on the gantry ground; the Mortar's three reticles are faint by design | |
| 2 | [HUD right panel](../../ui/hud/README.md) | the power row (spare power as a bar, the regen bonus), the weapons box with a row per slot (F, R, L, R, the name and five level pips) and the overdrive timer, in the round-13 kit | `hud-capture-final-r14-a` | no overdrive in Level 01, so the timer shows empty here; the mock's slot letters use R for both rear and right | |

## Notes

- Review file names: `<subject>-final-r14-a.png/.gif` and game captures
  `<subject>-capture-final-r14-a.png`; their `prompts.md` entries say how they are made. Nothing in
  `assets/` is hand-edited. The captures are of Level 01 with the `--loadout` debug option, auto-fire on.
- The sounds are the families' chosen recorded files, already rebuilt from the originals in round 12.
- The shared sprite pages still pack into one 2048×1024 page.
