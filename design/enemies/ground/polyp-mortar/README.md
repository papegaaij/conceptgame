---
title: Polyp Mortar
design: approved
implementation: in-progress
art: final
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-04
---

# Polyp Mortar

## Summary

A slate acid mouth ringed by tentacles that lobs acid blobs at the player's position; the impact point is marked a second ahead and bursts into a small ring.

## Design

### Stat block

Values are first-draft balancing numbers at **medium**, in [data.yaml](data.yaml) (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis.

<!-- data: stat-block -->
| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `ground` |
| Size tier | `small` |
| Size | 44×44 px, hitbox 32×32 |
| Parts | single |
| Orientation | `fixed` |
| HP | 10 (easy 8 / hard 13, from the global multipliers) |
| Armour / shield | none |
| Speed | 0 px/s (scrolls with the ground) |
| Movement | `terrain` |
| Attack | `mortar` every 3.5 s at the player's position (first 1.5 s after it enters): a lime impact marker shows 1 s ahead; the blob (not shootable) lands and bursts into a `ring` of 8 bullets (110 px/s, `small` = 4); a ship inside the 32 px impact circle when it lands takes the direct hit `heavy` = 10 |
| Formations | turret nest (pairs or with Spine Turrets) |
| Weak points | lime mouth (drawn only) |
| Effective traits | `anti-ground`, `area` |
| Credits | 15 (score 150 × chain) |
| Death | `small` wet burst; acid splash decal on the ground layer |
| First level / used in | L05; Acts 1–2 |
| Difficulty hooks | hard: 12-bullet ring |
<!-- /data -->

### Behaviour

- The marker tracks the player's position at launch, not during flight: keep moving.
- The first lob leaves 1.5 s after it enters the screen, then one every 3.5 s. The blob flies
  1.0 s (the marker's lead) and cannot be shot down.
- A **direct hit** is the ship inside the 32 px impact circle when the blob lands; the ring
  bursts either way.
- Its weak point is drawn only (single-part rule of [enemies](../../README.md)).
- The acid splash decal (`polyp-mortar-splash`, 40×40, `tools/art/l05_props.py`) stays on the
  ground for 10 s: fresh for 1 s, then 7 s, fading over the last 2 s.

## Concept art

Chosen concept: [polyp-mortar-r04-a.png](../concept/polyp-mortar-r04-a.png) (listed in the [ground](../README.md#concept-art) Concept art table). Production review files and their brief: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/polyp-mortar-final-r21-a.png](concept/polyp-mortar-final-r21-a.png) | Final sprites (`tools/art/l05_hazards.py`): the 8-frame idle pulse loop at 44×44 (orientation fixed), the acid blob (4 frames, 16×16) and the impact marker (radius 16 px, additive) | chosen |
| [concept/polyp-mortar-final-r21-a.gif](concept/polyp-mortar-final-r21-a.gif) | The mortar pulsing while a lob arcs over to its pulsing marker | chosen |
| [concept/polyp-mortar-death-final-r21-a.png](concept/polyp-mortar-death-final-r21-a.png) | Final death effects (`tools/art/l05_hazards.py`): `polyp-mortar-death` (12 frames, 64×64, additive: a lime flash and a spray of glowing acid droplets) and `polyp-mortar-tatters` (12 frames, solid: the nine tentacles torn off and tumbling, five dark shell shards), each at 4 steps a frame, with the small burst | chosen |
| [concept/polyp-mortar-death-final-r21-a.gif](concept/polyp-mortar-death-final-r21-a.gif) | The mortar dying on regolith with its small burst, the tatters under the glows | chosen |

## Implementation

- [x] Mortar lob with 1 s marker (`Lob`; the marker ring and the acid blob as sprites)
- [x] Ring on impact (starting 12 px outside the impact circle, so a direct hit is not doubled
      by the ring as it forms; 12 bullets on hard)
- [x] Direct-hit damage (the ship within the 32 px circle, a diameter, when it lands)
- [x] Stat block values loaded from data; global difficulty multipliers applied
- [x] Death effect, bounty and score per this spec (the small burst with its `-death` glow and
      `-tatters` pieces, the rocks, and the acid splash decal `polyp-mortar-splash` left on the
      ground for 10 s: fresh 1 s, 7 s, fading 2 s; all final in round 21)
- [ ] Glow frames (the lime mouth) for Level 06's darkness — M4 part F

## Decisions

- 2026-10-01: Promoted from the ground roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-03: M4 part E (main-agent choice): the stat block moved into [data.yaml](data.yaml) with the planned `mortar` pattern (marker 1.0 s, impact circle 32 px, ring 8 / 12 on hard); first lob 1.5 s after entering; the blob is not shootable; the weak point is drawn only (its ×1.5 dropped under the 2026-10-02 single-part rule).
- 2026-10-04: M4 part E batch, review files for round 21: `tools/art/l05_hazards.py` renders the
  mortar at its 44 px stat-block size (an 8-frame idle pulse; no firing frame, the game has no
  per-unit fire state and the lob is the shot), the acid blob and the impact marker, drawn by the
  game instead of the spore mine's frames and the dotted ring.
- 2026-10-03: M4 part E (user decision): props without a concept (the acid splash decal) get placeholders now and a/b variants in the part's production round.
- 2026-10-03: M4 part E, flown in Level 05 (implementation choices): the 32 px impact circle is
  its diameter; the ring starts 12 px outside it; the lob's marker stays where the ship was at
  launch, in the play plane (it does not scroll with the ground); the blob keeps flying when the
  mortar dies. Placeholder sprite cut from the round 04 sheet (`PlaceholderSprites`, 36 px).
- 2026-10-04: The death effect, missing from the batch, added to `tools/art/l05_hazards.py` as
  production art: `polyp-mortar-death` (a lime acid spray, additive) and `polyp-mortar-tatters`
  (its tentacles and shell shards, solid), which the game picks up by name with the small burst;
  review files `polyp-mortar-death-final-r21-a` for round 21. The acid splash decal's a/b concepts
  are in Level 05's concept directory.
- 2026-10-04: Round 21 verdict (user): the production sprites (`polyp-mortar-final-r21-a`) and the
  death effects (`polyp-mortar-death-final-r21-a`) approved as **final**; acid splash decal b
  (etched burn with a teal pool) chosen in Level 05's concept directory; sounds lob a and impact a.
- 2026-10-04: The acid splash decal's production art (`tools/art/l05_props.py`, concept b,
  review `l05-props-final-r21-c` in Level 05's concept directory, accepted with round 21's close):
  `polyp-mortar-splash`, three frames the game shows as the mortar's remains, spread over the
  remains' 10 s.
- 2026-10-04: M4 part F (user decision D2): in Level 06's darkness only the ground layer and the
  ground units are darkened, by a light map; this unit gets **glow frames** (its emissive lime mouth
  at full brightness, drawn after the light pass), so it is seen by its glow in the dark. A
  derived production pass of its final art, reviewed only.
