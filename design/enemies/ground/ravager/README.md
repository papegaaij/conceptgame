---
title: Ravager
design: approved
implementation: done
art: final
depends-on: [../../../systems/difficulty, ../../../systems/economy, ../creeper]
updated: 2026-10-07
---

# Ravager

## Summary

An animal-like Vrell pack hunter: a rust hound-raptor of chitin and sinew that gallops in packs across streets and plains and pounces at the player. The one ground unit that can touch the ship: for the middle of its leap it is in the air.

## Design

### Stat block

The numbers live in [data.yaml](data.yaml). Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis. Its HP is set at its first level (L09, reference DPS 63: time-to-kill 0.25 s, a fast and fragile hunter); where it returns (L14) it gets the act HP factor of a `medium` unit. The pounce is built (M5 part C) and read from the data file.

<!-- data: stat-block -->
| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `ground`; `air` for the middle 0.3 s of a pounce |
| Size tier | `medium` |
| Size | 56×56 px, hitbox 40×28 |
| Parts | single |
| Orientation | `16 angles` (16 headings × 8 gallop frames; it faces where it runs; leap frames for the pounce) |
| HP | 16 (easy 12 / hard 21, from the global multipliers) |
| Armour / shield | none |
| Speed | 160 px/s (plus scroll) |
| Movement | `walk` (a gallop) along its own authored ground path in a pack; turns at 180°/s; the gallop's 8 frames advance with distance (46 px per cycle) |
| Attack | `pounce`: within 200 px of the ship it leaps over the ship's position over 0.75 s, aimed twice as far so the middle of the leap passes over that spot, drawn up to 1.43× with its shadow sliding away; for the middle 0.3 s it is on `air` and deals contact `medium` = 15 once; then it lands behind it (kept on the play field) and runs on; 3 s between pounces; no ranged attack |
| Formations | pack (3–5) (each unit on its own path, 0.25 s apart) |
| Weak points | glowing teal maw (drawn only) |
| Effective traits | `anti-ground`, `spread` |
| Credits | 18 (score 180 × chain) |
| Death | `small` organic burst; no husk |
| First level / used in | L09; returns L14 (with the act HP factor) |
| Difficulty hooks | hard: 2 s between pounces; packs +1 (level data) |
<!-- /data -->

### Behaviour

- **Pack** (the new formation `pack`, see the
  [formation vocabulary](../../README.md#formation-vocabulary)): 3–5 Ravagers enter together, each
  on **its own** authored ground path, 0.25 s apart (a convoy's walkers share one path 1.5 s
  apart). A wave gives one path per unit, in screen coordinates at the wave's time, scrolling with
  the ground, as for every walker; a difficulty that adds a unit needs a path for it.
- **Gallop.** It runs its path at 160 px/s on top of the scroll, turning at most 180°/s, the
  nearest of its 16 headings drawn and its 8 gallop frames advancing with distance (46 px of
  ground per cycle, the concept's stride). Its paths run **across or obliquely down** the screen:
  straight down it would move at 160 + 150 px/s and cross the field in about 1.7 s.
- **Pounce** (user decision D4 = a of M5 part C). When the ship's centre is within **200 px** of
  its own, it leaps **over** the ship's position **at take-off** (no homing) over **0.75 s**: the
  leap aims at take-off + 2 × (ship − take-off), so its middle (the apex and the air window) passes
  over where the ship was and it lands as far beyond it (the overshoot; a landing point off the play
  field is pulled back onto it, the centre at least half its hit box inside every edge, and the
  second half of the leap is then shorter). It is drawn up to 1.43× at the apex with its shadow
  sliding away from it (the leap frames: front legs reaching, jaws open). A ship that holds still
  is touched; one that moves away is not. For the **middle 0.3 s** (0.225–0.525 s into the leap) it is fully on the **`air`**
  layer: it touches the ship and Rook (contact `medium` = 15, split over shield and armour like any
  collision, **once per pounce**, and it is not destroyed by it), every weapon that reaches `air`
  hits it at ×1 (no anti-ground ×2), ground-only blasts (dropped bombs, lobbed shells, Rook's
  Mortar) miss it, and mines can trigger on it. The rest of the leap it is `ground` (anti-ground
  bolts ×2, blasts hit). It lands, turns back onto its path at its turn rate and runs on; it may
  pounce again **3 s** after landing (hard 2 s). It pounces only while it is on the screen.
- **Weak point drawn only** (single-part rule, M5 part C default as for the Creeper): the teal maw
  glows but takes no extra damage; the draft's ×1.5 is struck.
- **Death.** A `small` organic burst; no husk.
- Ground units never collide with the player otherwise; the pounce's air window is the one
  deliberate exception, shown by the scale-up and the shadow's separation.

### Concept art

Chosen concept: [ravager-r05-a.png](../concept/ravager-r05-a.png), [ravager-r05-a.gif](../concept/ravager-r05-a.gif) (listed in the [ground](../README.md#concept-art) Concept art table). M5 part C takes it straight to production (user decision D9 = a): 56×56, 16 headings × 8 gallop frames, the leap frames with the scale-up to 1.43×, the separate shadow, the `small` death and a 30×30 intel portrait, approved as final in concept round 31 (user, 2026-10-07); its pounce sound, an a/b in the same round, keeps both and picks one at random per pounce ([sfx](../../../audio/sfx/README.md#enemies)).

The production files, generator notes in [concept/prompts.md](concept/prompts.md):

| File | What | Status |
|---|---|---|
| [concept/ravager-final-r31-a.png](concept/ravager-final-r31-a.png) | Final sprites (`tools/art/ravager.py`): 16 headings × 8 gallop frames (`ravager_0..127`, 56×56, 46 px per cycle), maw and eye glow masks, leap frames at 16 headings × 4 lift steps (`ravager-leap_0..63`, 80×80, 1.11–1.43×), the pounce as drawn with its shadow, the 10-frame `small` death (80×80) | chosen |
| [concept/ravager-final-r31-a.gif](concept/ravager-final-r31-a.gif) | A pack of four galloping across Level 08's night avenues 0.25 s apart; the lead pounces, lands, runs on and is shot | chosen |

## Implementation

- [x] Stat block values loaded from [data.yaml](data.yaml); global difficulty multipliers applied
- [x] `pack` formation: one path per unit, 0.25 s apart
- [x] Pack gallop with distance-driven frames (46 px per 8-frame cycle), turn rate 180°/s
- [x] Pounce: 200 px trigger, 0.75 s leap over the ship's take-off position (the overshoot), the per-unit layer (`air` for the middle 0.3 s) read at every hit, contact and targeting site, contact once per pounce with the ship and Rook, 3 s between pounces (hard 2 s)
- [x] Leap scale-up and shadow separation
- [x] Death effect, bounty and score per this spec
- [x] Production sprites (gallop, leap, shadow, death) and intel portrait (art track, concept round 31)
- [x] Pounce sound: round 31's a and b, one picked at random per pounce

## Decisions

- 2026-10-01: Promoted from the ground roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: HP rescaled to the lowered L09 reference DPS (20 → 16, ×63/80); time-to-kill stays ≈ 0.25 s.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-07: M5 part C (user decisions of 2026-10-07): **D4 = a** for the middle 0.3 s of a
  pounce the Ravager is fully `air` (contact `medium` 15 once per pounce with the ship and Rook,
  air-reaching weapons at ×1, ground blasts miss, mines trigger), the rest of the leap `ground`;
  rejected: b (always `ground` for hits: a bomb could hit it in mid-air) and c (the whole 0.75 s
  `air`: a contact that long is hard to dodge). Stated defaults: the numbers moved into
  [data.yaml](data.yaml) and the table is rendered from it; the weak point is **drawn only**
  (single-part rule), so the maw's ×1.5 is struck; `pack` joins the vocabulary (3–5 walkers, each
  on its own path, 0.25 s apart); turn rate 180°/s, the gallop's 8 frames by distance; the pounce
  aims at the ship's position at take-off, lands and rejoins its path; paths run across or
  obliquely; a `small` death, no husk. Our readings, for review in round 31: the 46 px stride (the
  concept generator's), the 200 px measured centre to centre, the air window at 0.225–0.525 s, the
  pounce only while on the screen, the cooldown counted from landing, contact not destroying it
  (it is `medium`, not a rammer). The pounce's keys are planned (the
  [schemas](../../../tech/architecture/README.md#data-file-schemas)): they wait in the data's
  comment until the simulation reads them. `design` goes to `review` for round 31.
- 2026-10-07: Overshoot (user): as first built the leap landed on the ship's take-off position, so
  in its middle 0.3 s on `air` it was still 60–140 px short and a ship that held still was never
  touched. The leap now aims at take-off + 2 × (ship − take-off): its middle, the apex and the air
  window, passes over the ship's take-off position and it lands behind it, then runs on. A landing
  point off the play field is clamped onto it (the centre at least half its hit box inside the
  edges; the leap's second half is then shorter), rather than letting it land off the screen and
  leave, which would count as an escape (Level 09's bridge secondary). D4 otherwise unchanged.
- 2026-10-07: Time to kill (user): 16 HP stays; its 0.25 s at the L09 reference DPS, under the
  `medium` class's 0.4–1.5 s, is an accepted exception (a fast, fragile pack hunter): `BalanceTest`
  lists it as such instead of a deviation.
- 2026-10-07: M5 part C simulation built: the pounce and its keys are read from the data file (no longer a comment); the simulation items are ticked, the drawing and the art follow.
- 2026-10-07: [Concept round 31](../../../concept-rounds/round-31/README.md) closed for the Ravager
  (user, 2026-10-07): the production sprites (`ravager_0..127` with the gallop, the glow masks, the
  leap frames and their shadow, the `small` death with its tatters) and the intel portrait approved
  as **final**, the weak spots as they are (the same pose at all four lift steps, the faint leap
  shadow at 1:1, night contrast 3.1); its pounce sound keeps **both** a and b, the game picking one
  at random per pounce; the stat block's numbers, the time-to-kill exception, our readings (the
  46 px stride, the 200 px trigger, the air window, the cooldown from landing) and the overshoot's
  clamped landing (build choice c) accepted. `design: approved`, `art: final`; every Implementation
  item is ticked, so the implementation is `done`.
