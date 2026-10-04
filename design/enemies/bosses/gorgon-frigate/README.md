---
title: Gorgon Frigate
design: approved
implementation: in-progress
art: final
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-04
---

# Gorgon Frigate

## Summary

The Act 1 mid-boss and the first multi-part enemy: a bone-white medusa-bell warship with three serpent necks whose cobra heads are turrets. Kill the heads one by one, then the crown opens over the lime core.

## Design

### Stat block

Values are first-draft balancing numbers at **medium**, in [data.yaml](data.yaml) with the boss script (`boss`: phases, par, the neck chains) (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis.

<!-- data: stat-block -->
| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `air` |
| Size tier | `large` |
| Size | 300×320 px, hitbox 200×120 |
| Parts | bell (`armoured`), 3 necks of 5 `armoured` segments, 3 cobra heads (`destroyable` turrets), crown petals + core (`vital`, exposed in phase 3) |
| Orientation | `32 angles` (neck segments and heads; the bell 32 angles for its sway) |
| HP | head 220 each; core 840; total 1500 (easy 1125 / hard 1950, from the global multipliers) |
| Armour / shield | bell and neck segments armoured (sparks, no damage) |
| Speed | 60 px/s (the descent), then `hover` |
| Movement | descends at 60 px/s (invulnerable), then `hover` with bell centre at y 110 and a slow `sine` (amplitude 40 px, period 6 s); the necks bend toward the player with lagged follow-through |
| Attack | see *Phases*: phase 1: the heads take turns, one 3-shot `aimed` burst every 1.2 s (0.15 s apart, 160 px/s, `small` = 4); phase 2: the last head bursts every 0.8 s; phase 3: the core fires a 12-bullet `ring` (110 px/s); 2 s later a slow 3-arm `spiral` for 3 s (90 px/s, turning 60°/s), then the next ring |
| Formations | carrier + escorts (Skitter streams of 6 in phase 1) |
| Weak points | heads' lime eyes (×1.5), lime core (×2) |
| Effective traits | `forward`, `piercing`, `spread` |
| Credits | 200 (score 2000 × chain) (heads 30 each, core 110; ≈ 15 % of the L05 budget, 1,311) |
| Death | `medium` chained bursts at each part, a cluster of `large` blasts over the bell and necks; the bell breaks into five wedges and the necks fall away, drifting apart and fading; crown petals tear off; credit shower |
| First level / used in | L05; mid-boss of L05 only |
| Difficulty hooks | hard: the last head fires 5-shot bursts; core rings of 16 bullets |
<!-- /data -->

### Phases

| Phase | Ends at | Behaviour |
|---|---|---|
| 1 — Three heads | two heads dead (≈ 71 % of the total HP left) | The heads take turns: one 3-shot `aimed` burst (0.15 s apart, 160 px/s, `small` = 4) every 1.2 s, rotating through the living heads. A Skitter stream of 6 (0.5 s apart, alternating side edges) at the settle and every 10 s, in phase 1 only. |
| 2 — Last head | the last head dead (≈ 56 %) | The remaining head is enraged: a burst every 0.8 s, 5-shot on hard; its neck lashes wider (55° instead of 30°). |
| 3 — Core | core 840 → 0 | Crown petals open over the lime core, which only now takes damage: a `ring` of 12 (110 px/s; 16 on hard), 2 s later a slow 3-arm `spiral` (a bullet per arm every 0.25 s, 90 px/s, turning 60°/s) for 3 s, then the next ring. Armour sparks on the bell remain. |

Phases end on the head count, not on an HP share: the percentages follow from the numbers
(heads 3 × 220, core 840).

### Arena

Over the Vrell nest crater on Luna ([L05](../../../campaign/act-1-first-contact/level-05-crater-nest/README.md)), after the four nest batteries: it arrives at a fixed t = 150 s and the scroll slows to 30 px/s; no terrain collision. The arena section lasts 55 s; if the frigate still lives then, the scroll halts (the level clock pauses) until it dies, and the level's next section starts at its death. Target duration 45–75 s at medium.

- **Intro**: it descends from above the top edge at 60 px/s, invulnerable, until the bell's centre reaches y 110 px; the mini-boss sting, its name and the short mid-boss bar appear as it enters. The necks reach down past the upper third (the bell holds it).
- **Par**: 60 s at every difficulty, from the bar appearing to the kill; under par pays the [Boss rush](../../../systems/scoring/README.md#level-end-bonuses) bonus (mid-bosses count).
- **Boss bar**: the sum of the heads' and the core's remaining HP, at the top of the play field ([HUD](../../../ui/hud/README.md#in-the-play-field)).

### Behaviour

- The first boss-style health bar appears (mid-boss bars are shorter). A radio line introduces it.
- Destroyed heads leave smoking stumps that leak ichor; the neck goes limp.

## Concept art

Chosen concept: [gorgon-frigate-r06-a.png](../concept/gorgon-frigate-r06-a.png), [gorgon-frigate-r06-a.gif](../concept/gorgon-frigate-r06-a.gif) (listed in the [bosses](../README.md#concept-art) Concept art table). Production review files and their brief: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/gorgon-frigate-final-r21-a.png](concept/gorgon-frigate-final-r21-a.png) | Final sprites (`tools/art/gorgon_frigate.py`): the frigate as the game composes it (at rest, necks swinging, phase 3), the data's hit boxes, the bell in 4 crown stages (240×200), the head and its stump and the 5 tapering neck pieces at 15 headings each (±78.75° of straight down), the death's crown petals (6 frames, solid) and ichor (10 frames, additive), the core glow; one 48-colour palette | chosen |
| [concept/gorgon-frigate-final-r21-a.gif](concept/gorgon-frigate-final-r21-a.gif) | The necks swinging with lag, the heads destroyed one by one, the crown opening over the pulsing core, the petals tearing off in the ichor | chosen |
| [concept/gorgon-frigate-death-final-r21-b.png](concept/gorgon-frigate-death-final-r21-b.png) | Death redo, round 21 item 11 (`tools/art/gorgon_frigate_death.py`): the three necks and the five bell wedges at their offsets beside the body at its death, the break-up at four steps, each chunk's 3 tumble frames, the blast layout | chosen |
| [concept/gorgon-frigate-death-final-r21-b.gif](concept/gorgon-frigate-death-final-r21-b.gif) | The whole death: the part chain, the blast cluster peaking at the swap, the petals tearing off, the chunks drifting apart, sinking, darkening and fading under trailing blasts | chosen |

## Implementation

- [x] Neck chains with lagged follow-through and per-head HP
- [x] Three phases ending on the head count; core invulnerable until phase 3; Skitter streams in phase 1
- [x] Lime weak points (×1.5 eyes, ×2 core); health bar; credit shower (plain shapes until the production sprites)
- [x] Invulnerable descent, par 60 s for the Boss rush bonus; arena clock halts while it lives after 55 s
- [x] Stat block values loaded from data; global difficulty multipliers applied
- [x] Death effect, bounty and score per this spec (bounty, score, the chained bursts with the ichor cloud, the crown petals tearing off)
- [x] Production sprites drawn by the game (`BossLooks`: neck pieces and heads from heading sets, the bell's crown stages, the core glow, the stumps)
- [x] Break-up at the death (round 21 redo, the Leviathan's `SetPieceDeath` with the `death` table in
      `assets/pivots/gorgon-frigate.json`): the wreck where it died until the swap, then three necks
      and five bell wedges drifting apart; the blast cluster and trailing blasts; the petals at the
      swap; the layered explosion sounds

## Decisions

- 2026-10-01: Promoted from the bosses roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: Arena set to the nest crater on Luna (L05), replacing the Earth–Moon convoy lane.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-03: M4 part E (user decisions): the frigate arrives at a fixed t = 150 s (after battery D, not triggered by it); the arena scrolls at 30 px/s for 55 s, then halts until the frigate dies; par 60 s for the Boss rush bonus, which mid-bosses earn too.
- 2026-10-03: M4 part E (main-agent choice): the stat block and boss script moved into [data.yaml](data.yaml); phases end on the head count, so phase 1's "100–60 %" is now "≈ 71 %"; invulnerable during the descent; Skitter streams at the settle and every 10 s in phase 1 only; the hard core ring is 16. Chosen numbers: bell hit box 200×120, head hit boxes 36×36 at rest 170–190 px below the bell's centre, core 60×60, neck segments 22×22 with 0.12 s lag and 30° bend (55° enraged), stream interval 0.5 s, spiral 3 arms, a bullet per arm every 0.25 s, 90 px/s, 60°/s.
- 2026-10-03: M4 part E step 2: the frigate flies as the first boss (see the
  [architecture](../../../tech/architecture/README.md) log): it arrives at the level's `boss` time,
  descends at 60 px/s from above the top edge, settles with its bell at y 110 and sways ±40 px over
  6 s; each neck's anchor eases toward the ship within its bend and every piece after it follows
  0.12 s late; the rotating head bursts, the last head's rage, the core's ring and spiral in turn
  after a 1 s crown opening, and the Skitter streams (side stream paths, alternating edges) run per
  the data. Autopilot kill times on the Level 01–based test level with a level-4 Pulse Cannon:
  easy 34 s, medium 44 s, hard 66 s. Drawn in plain shapes (no sprite cut of the concept exists).
- 2026-10-04: M4 part E batch, review files for round 21: `tools/art/gorgon_frigate.py` renders the
  frigate from the chosen round-06 model, the bell re-laid to the data (sockets at the chain anchors,
  core at its offset). The neck pieces are a small heading set rather than a round segment, so the
  ribbed segments keep their dorsal seam and spines: the 15 of the 32 angles within ±78.75° of
  straight down, enough for the 9.5° rest plus the 55° enraged bend. The bell's crown comes as 4
  whole-bell stages (closed, two opening, open) so the petals' shadows stay right; the death adds
  the petals tearing off (solid) and a lime-violet ichor cloud, the latter played with every part's
  burst like the Leviathan's.
- 2026-10-04: Round 21 verdict (user): the sprites are right in the game, but the death "needs more
  explosions and pieces; now it just disappears". Redone the Leviathan's way
  (`tools/art/gorgon_frigate_death.py`, review `gorgon-frigate-death-final-r21-b`, round 21 item
  11): the body stays (crown open, core dark) until the swap at step 54 (0.9 s), under the part
  chain and a cluster of 13 large and 6 medium blasts and two ichor clouds over the bell and the
  necks that peaks there; then it is replaced by eight chunks — the three necks with their stumps
  falling away and the bell cut into five wedges round the core along jagged seams (charred, the
  torn flesh glowing along the broken edge and in veins down the cut face), each with two tumble
  frames rendered under the fixed light — that drift apart, sink to 0.85×, darken and fade by step
  160 under 8 trailing blasts; the petals tear off at the swap, and the break-up's explosion
  sounds are layered as for the Leviathan.
- 2026-10-04: Round 21 closed (user): the death redo (`gorgon-frigate-death-final-r21-b`, item 11)
  accepted; with the sprites (`gorgon-frigate-final-r21-a`) the frigate's art is **final**.
