---
title: Mantis
design: approved
implementation: in-progress
art: chosen
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-05
---

# Mantis

## Summary

A bone-white Vrell sniper that enters from a side edge, holds position and sweeps a short laser across the lower screen. The first enemy that demands `side` or `spread` weapons.

## Design

### Stat block

The numbers are in [data.yaml](data.yaml); the table is rendered from it. Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis.

<!-- data: stat-block -->
| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `air` |
| Size tier | `medium` |
| Size | 80×80 px, hitbox 40×56 |
| Parts | single |
| Orientation | `fixed` (facing inward, nose down and leaning into the field; its own render per side, nothing mirrored) |
| HP | 26 (easy 20 / hard 34, from the global multipliers) |
| Armour / shield | none |
| Speed | 150 px/s entering and leaving, 0 while hovering |
| Movement | enters from the left or right edge (edge-warned like every side entry), `hover` at x = 40 px from that edge, y = 120–360 px, for 6 s (two sweeps), then exits the way it came |
| Attack | `laser-sweep` from its eye: a beam 300 px long and 6 px wide sweeps a 70° arc over 1.2 s, centred on the ship's bearing when the telegraph starts (clamped between straight inward and straight down, so the far side of the field stays safe); telegraphed 0.6 s by the crimson arc; every 3 s while hovering, the first 0.6 s after it settles; damage `laser` = 8 per touch (once per sweep) |
| Formations | single, pincer (the pincer one from each side) |
| Weak points | crimson thorax glow (×1.5) |
| Effective traits | `side`, `spread` |
| Credits | 30 (score 300 × chain) |
| Death | `medium` burst: bone shards, crimson flash |
| First level / used in | L06; Acts 1–2 |
| Difficulty hooks | hard: 90° arc, every 2.5 s |
<!-- /data -->

### Behaviour

- The Mantis hovers outside the reach of forward guns unless the player moves close to the edge — the intended dilemma.
- Never fires from off-screen; it must be fully visible before the first telegraph.
- **The beam** (user decision D4 of M4 part F): 300 px long and 6 px wide, from the eye. The
  sweep is centred on the ship's bearing when the 0.6 s telegraph starts, clamped between straight
  inward and straight down, so the far side of the field is out of reach: getting away from that
  side is a real answer. The crimson telegraph wedge shows the full sweep (both edges and the
  length) and the beam is drawn while it sweeps; it hits the ship at most once per sweep.
- **From the head** (round 23): the beam, its hit test, its aim (the ship's bearing) and the
  telegraph start at the eye, `sweep.origin` in [data.yaml](data.yaml): 14 px toward the field and
  25 px down from the unit's centre (mirrored on the right edge), the eye of the production sprite
  in its sweep pose (nose down, leaning 30° in). The telegraph frames lean 22–30°, so there the eye
  is up to 4 px off that point.
- **The look** (round 23, variant B "charged lance"): a crimson beam strip with energy knots running
  out from the eye, a spark at its tip and a ring on the eye; the telegraph is one faint filled
  wedge with a bright rim on the arc and the edges (the danger zone as an area, pulsing), one
  pre-rendered wedge per arc (70°, hard's 90°). All additive.
- Two sweeps per 6 s hover (telegraphs 0.6 s and 3.6 s after it settles; on hard every 2.5 s, still
  two: a sweep starts only if it ends before the hover does). It enters alone from a side edge
  (`single` from a side, or one per edge in a pincer) with the 3 s side-entry warning of every side
  and rear entry, and leaves through the edge it came from.

## Concept art

Chosen concept: [mantis-r04-a.png](../concept/mantis-r04-a.png) (listed in the [air](../README.md#concept-art) Concept art table). Production review files, the beam concepts and their briefs: [concept/prompts.md](concept/prompts.md); reviewed in [round 23](../../../concept-rounds/round-23/README.md).

| File | What | Status |
|---|---|---|
| [concept/mantis-final-r23-a.png](concept/mantis-final-r23-a.png) | Final sprites (`tools/art/mantis.py`): 80×80, the round-04 model nose down leaning into the field, its own render per edge (left and right, nothing mirrored): hover wing beat (4), telegraph with the arms opening and the eye charging (4), sweep (2), exit leaning out (4); the death's crimson flash (additive) and bone shards, wings and arms (solid), 12 frames each; 40 colours | proposed |
| [concept/mantis-final-r23-a.gif](concept/mantis-final-r23-a.gif) | A pincer: entering, hovering, two telegraphs and sweeps (the beam as the game draws it today), leaving; then the death | proposed |
| [concept/mantis-beam-r23-b.png](concept/mantis-beam-r23-b.png), [.gif](concept/mantis-beam-r23-b.gif) | Beam and telegraph B: charged lance, filled wedge telegraph (`tools/concept/props_r23.py`) | chosen |
| [concept/mantis-beam-final-r23-a.png](concept/mantis-beam-final-r23-a.png), [.gif](concept/mantis-beam-final-r23-a.gif) | Final beam and telegraph from the chosen B (`tools/art/mantis_beam.py`): beam strip 32×10 (4 frames, knots running out), tip spark 16×16 (4), eye ring 16×16, telegraph wedges 302×349 (70°) and 302×429 (90°), all additive; in play with the production sprites of both edges, from the eye | proposed |
| [concept/mantis-beam-capture-r23-b.png](concept/mantis-beam-capture-r23-b.png) | Game capture, Level 06's pincer: the telegraph wedges and the sweeps from the eye on both edges | proposed |
| [concept/rejected/mantis-beam-r23-a.png](concept/rejected/mantis-beam-r23-a.png), [.gif](concept/rejected/mantis-beam-r23-a.gif) | Beam and telegraph A: hot wire, dotted arc and dashed edges | rejected — user picked b |

## Implementation

- [x] Side entry with edge warning (a single unit from a side edge), hover at x 40 from the edge
- [x] Laser sweep with 0.6 s arc telegraph: 300 px × 6 px beam centred on the ship's bearing,
      clamped inward-to-down, once per sweep; telegraph and beam drawn (placeholder strokes until
      the part's concept round)
- [x] Exit after 6 s
- [x] Stat block values loaded from data; global difficulty multipliers applied
- [x] Death effect, bounty and score per this spec (the bone shards and crimson flash of
      `tools/art/mantis.py` with the medium burst)
- [x] Production sprites drawn by side and state (`FarsideLooks.mantisFrame`: hover, telegraph,
      sweep, exit; the left and right edge each their own render); review in round 23
- [x] The beam, its hit test, its aim and its telegraph start at the eye (`sweep.origin`, round 23;
      `MantisBeamTest`, `MantisBeamDataTest`)
- [x] The beam and telegraph drawn as the look chosen in round 23 (variant B,
      `tools/art/mantis_beam.py`: the wedge per arc, the tiled beam strip, tip spark and eye ring,
      additive, in `FarsideLooks.drawSweeps`; strokes only where the art is missing)

## Decisions

- 2026-10-01: Promoted from the air roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-04: M4 part F: the stat block moved into [data.yaml](data.yaml) (`laser-sweep` with its
  `sweep` block, `hover` `edge_x` and `exit: back`, the hard hook `sweep_arc`; planned keys in the
  [schemas](../../../tech/architecture/README.md#data-file-schemas)). The 1.5 s edge warning
  follows the code's 3 s minimum for every side and rear entry (main-agent choice).
- 2026-10-04: The beam is 300 px long and sweeps centred on the ship's bearing, with the 0.6 s
  arc telegraph (user decision D4 of M4 part F); rejected: a full-width beam (the edge dilemma
  disappears, only a dodge) and a fixed inward-down sweep (predictable, weaker).
- 2026-10-04: M4 part F step 2 built the side hover, the sweep and its telegraph (`FarsideTest`,
  `FarsideLevelTest`). The hard sweep interval of 2.5 s is authored, so the fire-rate lever does
  not apply on top of it; on easy the 3 s interval is divided by the lever (main-agent choice).
- 2026-10-04: M4 part F batch (round 23): production sprites from the chosen round-04 model
  (`tools/art/mantis.py`, 80×80, 28 frames, death 2 × 12). The data's "mirrored per side" is
  rendered as two sets instead, since nothing lit is mirrored at runtime; the Mantis hangs nose down
  and leans 20° into the field (30° while it telegraphs and sweeps, 25° out as it leaves), so its
  56 px-tall hit box still fits. The beam is drawn from the unit's centre (the sim's origin), about
  30 px above its eye; the beam's look is the round-23 a/b concept (D8).
- 2026-10-05: Round 23 (choices 1 and 6, user): the beam starts at the head, not the body: the
  sweep's `origin` [14, 25] (px toward the field and down, mirrored on the right edge) is the eye of
  `tools/art/mantis.py`'s sweep pose, and the beam, its hit test, its aim at the ship and the
  telegraph start there. The beam's look is variant B, the charged lance with the filled wedge
  telegraph, made final by `tools/art/mantis_beam.py`; rejected: variant A, the hot wire with the
  dotted arc (user picked b).
