---
title: Skitter
design: approved
implementation: in-progress
art: final
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-08
---

# Skitter

## Summary

Swarm fodder of grown chitin: tiny, fast, one hit to kill. It teaches the player to shoot and to read a stream of enemies, and it pads most Vrell waves.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis.

<!-- data: stat-block -->
| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `air` |
| Size tier | `tiny` |
| Size | 24×24 px, hitbox 16×16 |
| Parts | single |
| Orientation | `16 angles` (turns to face its direction of flight; 6 wing-beat frames per heading) |
| HP | 1 (easy 1 / hard 1, from the global multipliers) |
| Armour / shield | none |
| Speed | 190 px/s (stream 160, swoop up to 240) |
| Movement | `snake` along an authored `path` (unit spacing 0.25 s), `swoop` (entry curve radius 120–200 px), `straight` in streams |
| Attack | `none` — contact only (contact damage `tiny` = 6) |
| Formations | snake (6–12), stream, line abreast (5–10), swarm |
| Weak points | none (dies in one hit) |
| Effective traits | `spread`, `forward` |
| Credits | 5 (score 50 × chain) |
| Death | `tiny` pop: chitin flakes, teal glow flash; no drop |
| First level / used in | L01; recurring fodder through Acts 1–2 and the Brood Pod / Brood Carrier / Hive Node spawns |
| Difficulty hooks | none beyond the global levers |
<!-- /data -->

### Behaviour

- Skitters never fire. Their threat is the path: snakes that sweep across the player's lane and streams that alternate edges.
- A snake always enters from off-screen along its spline; its head is on screen at least 1.5 s before it can reach the player, so the player can read it (see the `snake` formation).
- Spawned Skitters (Brood Pod, Brood Carrier, Hive Node) leave their spawner on a short `swoop` toward the player, then continue `straight` and exit.

### Concept art

Chosen concept: [skitter-r04-a.png](../concept/skitter-r04-a.png) (listed in the [air](../README.md#concept-art) Concept art table).

## Concept art

Production art for concept round 12 (the Level 01 batch; part P2 opens the round), review files built from the final frames in `assets/` by `tools/art/vrell_air.py` (`--review` rebuilds only them); prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/skitter-final-r12-a.png](concept/skitter-final-r12-a.png) | Final sprites: 16 headings × the 6-frame wing beat (96 frames, 24×24, 24 colours): the beat at two headings at 6×, every heading with the wings raised and spread at 3× and at 1× | chosen |
| [concept/skitter-final-r12-a.gif](concept/skitter-final-r12-a.gif) | A snake of five on a figure-eight, each showing the heading nearest its direction of flight, beating at 10 fps | chosen |
| [concept/skitter-death-final-r26-a.png](concept/skitter-death-final-r26-a.png) | Round 26 (M4 part H, `tools/art/vrell_deaths.py`): the death effect, `skitter-tatters_0..9` (32×32, solid: the wings torn off, the body cracked into two chitin flakes, the tail spikes) and `skitter-death_0..7` (32×32, additive teal glow flash, 4 steps after the tiny pop), with the pop and at 1× | chosen |
| [concept/skitter-death-final-r26-a.gif](concept/skitter-death-final-r26-a.gif) | Round 26: three Skitters dying one after the other, the pop, flakes and flash as the game layers them | chosen |

## Implementation

- [ ] Snake and swarm entry paths authored as data — **M5 part D** (user decision D8 = a of
  2026-10-08): a `snake` wave's `paths` (one route of `[x, y]` points, as a chain's) and a `swarm`
  wave's leader route, with the Mote Swarm's flock (Level 10) as the first swarm; streams keep the
  shapes `vanguard.sim.Formations` lays out (no level needs authored streams). Act 1's snakes keep
  their shapes too, a snake without `paths` flying one as before
- [x] Contact damage 6, split shield/armour per the collision rule
- [x] Pays 5 credits; counts toward chains
- [x] Stat block values loaded from data; global difficulty multipliers applied
- [x] Death effect, bounty and score per this spec (the death's production frames approved as final in round 26)
- [x] Turns to face its direction of flight: the nearest of 16 headings, turning at most one heading per game frame

## Decisions

- 2026-10-01: Promoted from the air roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-01: M1 implementation (`vanguard.sim.Skitter`, stat block in `SkitterSpec`): HP 1, 16×16 hit box, 190 px/s along hand-made snake paths of 6 with 0.25 s spacing (the whole snake enters within 1.5 s) in the temporary M1 test sortie. A Skitter that rams the ship is destroyed by the impact, so its contact damage lands once. Placeholder: the 3 wing-beat frames of `skitter-r04-a.png`, scaled to the stat block's 24×24 by `:pipeline:importPlaceholders` (the concept draws them at 30×30); death plays the 24 px tiny explosion of `explosions-r09-a.png` and `explosion-tiny-r03-a/b`, hits `hit-organic-r08-a/b`.
- 2026-10-01: User decisions from M1: the Skitter is 24×24 as in the stat block (the 30×30 concept is scaled for the placeholder; production art renders at 24); a ramming Skitter is destroyed by the impact; snakes of 6–12 stay allowed under the head-based readability rule.
- 2026-10-02: The stat block moved into [data.yaml](data.yaml) and is rendered from it (M2 data files); easy/hard HP come from the difficulty levers, the contact damage from the balancing basis, the score from the bounty. `vanguard.sim.SkitterSpec` is now built from it (`vanguard.content.SimSpecs`); the difficulty multipliers are not applied yet, so the checklist item stays open.
- 2026-10-02: M2: the Level 01 snakes, line abreast sweeps (front and rear) and streams fly
  as planned by `vanguard.sim.Formations`; a wave's `speed` (the slow 120 px/s waves) replaces the
  stat block speed, streams fly at 160 px/s. HP comes through the HP lever (1 on every difficulty).
  The entry paths are still authored in code (`Formations`), not as data, and swarms are not
  built, so that item stays open; a rammed Skitter pays its bounty like a kill.
- 2026-10-02: Line abreast widened to 5–10 Skitters (user decision), so Level 01's lines of 8
  (10 on hard) fit the stat block.
- 2026-10-02: Production art (Level 01 batch, `tools/art/vrell_air.py`): the 6-frame wing beat (24×24, 24 colours), rendered at 8× from the chosen round-04 model with one palette for the cycle (the placeholder had 3 frames, which snapped from the last back to the first). `orientation: fixed` in the stat block, so no heading set; the production plan's "angle sets" for Level 01's enemies are therefore not rendered (open point for the user). Review files proposed for round 12.
- 2026-10-02: Orientation `16 angles` (user decision, Level 01 batch part P3): the Skitter turns to
  face where it flies instead of flying nose-down. The final sprite is an angle set of 16 headings ×
  the 6 wing-beat frames (96 frames at 24×24, `tools/art/vrell_air.py`, each heading its own render
  with the key light fixed); the wing beat now lifts and sweeps the wings so it reads as a shape
  change. In the game `vanguard.sim.Enemy` keeps a presentation-only facing that turns toward the
  direction of each step's movement by at most 22.5° per step (art direction, Rotation) and is not
  part of the state hash, so replays are unchanged; `EnemyLooks` draws the nearest heading. The
  `orientation` field is typed (`vanguard.content.Orientation`).
- 2026-10-02: Concept round 12 closed (user decision): the 16-heading × 6-frame production sprites (`tools/art/skitter.py`) approved as **final**, `art: final`.
- 2026-10-05: M4 part H (round 26 batch): the death effect of its own, the tiny pop's chitin flakes (the wings and two body flakes) and a teal glow flash, rendered by `tools/art/vrell_deaths.py` and played by the game with the ladder burst (EnemyLooks picks up a slug's `-death` glow and `-tatters` pieces by name; no code change needed); review files proposed for [round 26](../../../concept-rounds/README.md); the part's `art` stays as it is until the user approves them there.
- 2026-10-05: Concept round 26 closed (user: the round accepted as proposed): the death effect's production frames (the chitin flakes and teal flash) approved as **final**; the part's art stays `final`.
- 2026-10-05: M4 close-out: the item "entry paths authored as data" is tagged **later: M5**, the
  rest of the checklist is done, so the Skitter is `done` for M4. Act 1 has no Skitter swarm and
  its snakes and streams fly as planned on the shapes `Formations` lays out; the authored paths come
  with the first levels that need them (Act 2's weaves and the `swarm` formation of M5).
- 2026-10-08: M5 part D (user decision D8 = a and the stated defaults): the authored-paths item is
  narrowed to **snakes and swarms**: a snake wave accepts `paths` (Level 10's Skitter snakes may use
  them) and the `swarm` formation's leader route is
  data (the Mote Swarm); streams keep their laid-out shapes. Ticked when the simulation reads them;
  the document is `in-progress` again until then.
