---
title: Concept round 15 — M4 part B, Level 02
design: review
implementation: n/a
art: proposed
updated: 2026-10-02
---

# Concept round 15 — M4 part B, Level 02

## Summary

The production-art round of M4 part B ([roadmap](../../tech/roadmap/README.md#m4-parts)): Level
02's new units, pickups, backdrop, hazard and music, rendered as **final** art by the generators in
[tools/art/](../../../tools/art/README.md) into `assets/`, and the stems of its theme. Open
[index.html](index.html) in a browser (regenerate with `python3 tools/concept/board.py 15`). Every
choice is *approve as final* or *redo* (with what to change); only an approved part gets
`art: final`. To fly the level: `./gradlew :desktop:run --args="--level 2"`.

## Choices

| # | Part | What is proposed | Review files | Weak spots (Claude) | Outcome |
|---|---|---|---|---|---|
| 1 | [Stinger](../../enemies/air/stinger/README.md) | the ±30° tilt set (7 headings × 4 wing-beat frames) and the crimson pause flare (`tools/art/vrell_air.py stinger`) | `stinger-final-r15-a` (png, gif) | at ±30° the far wing foreshortens strongly and catches more light | |
| 2 | [Spine Turret](../../enemies/ground/spine-turret/README.md) | the whole turret at 32 barrel headings and its scorched stump (`tools/art/vrell_ground.py`) | `spine-turret-final-r15-a` (png, gif) | the concept model's violet seam runs as a straight line through the bulb in every heading | |
| 3 | [Pickups](../../player/README.md) | salvage M and the overdrive (`tools/art/pickups.py`) | `pickups-final-r15-a` (png, gif) | | |
| 4 | [Level 02 backdrop](../../campaign/act-1-first-contact/level-02-shipyard-burning/README.md) | the burning yard: the north arm with fires and tracers, the docks with frigates and teal Vrell growth, the *Resolute* venting coolant, smoke and coolant banks, frost streaks, Earth's limb at the end (`tools/art/backdrop_l02.py`) | `backdrop-final-r15-a`, game captures | the Earth tile and the station kit are Level 01's unchanged; the north arm is a still (the motion budget) | |
| 5 | [Crane Four](../../campaign/act-1-first-contact/level-02-shipyard-burning/README.md) | its arm at 61 angles, the warning and clamp lights, the canister (`tools/art/crane_four.py`) | `crane-four-final-r15-a` (png, gif), game captures | the thin lattice jib reads dark on the dark yard | |
| 6 | ["Afterburner"](../../audio/music/README.md) stems | the base stem of the chosen full mix, sample-aligned (`tools/concept/audio/music_r15.py`) | `afterburner-base-r15-a.ogg`, *Listening* below | | |

## Notes

- Review file names: `<subject>-final-r15-a.png/.gif` in each part's `concept/`; their `prompts.md`
  entries say how they are made. Nothing in `assets/` is hand-edited.

## Listening

The full mix as chosen (round 08) and the new base stem, sample-aligned: the game plays the base
stem through sections 1–3 and fades the full mix in from section 4 (the main drydock).

| Stem | File | Loudness | Loop |
|---|---|---|---|
| Full mix | [afterburner-full-r08-a](../../audio/music/concept/afterburner-full-r08-a.ogg) | −14 LUFS | 623700 + 6048000 |
| Base | [afterburner-base-r15-a](../../audio/music/concept/afterburner-base-r15-a.ogg) | −1.4 LU under the full mix | 623700 + 6048000 |
