---
title: Level 14 – Siege Spire
design: review
implementation: in-progress
art: proposed
depends-on: [../../../enemies/bosses/siege-spire]
updated: 2026-10-12
---

# Level 14 – Siege Spire

## Summary

The Act 2 finale. A three-kilometre Vrell citadel, the **Siege Spire**, has grown through the
heart of **Geneva Concord**, the UTC capital, and its canopy shadows the whole old town. The
Coalition throws everything it has left at it. Lancer and Rook go in first, along the lakefront
and through the old town, where the Spire's roots burst out of the streets and grow guns, to the
Concord rotunda where the trunk stands. There they fight the act boss, the
[Siege Spire](../../../enemies/bosses/siege-spire/README.md). The boss sums up the act: ground
targets first, then attacks from the rear, then dodging in the open. 108 seconds of approach at
140 px/s, then the boss fight (tuned to about 135 s, measured in the content step), then a 12 s
daylight section, a 25 px/s drift.

**Status (M5 part H, step H4).** The level's [data.yaml](data.yaml) exists and the marked tables
below are rendered from it (step H4: the loader, the Spire's data, the measured fight); the backdrop
in the data is a placeholder until step H5. The design was decided on 2026-10-11 for M5 part H
(decisions H1–H10 and the stated defaults of the part's gap analysis, see *Decisions*). **The user is
on vacation: every decision here was taken as the recommendation and is pending the user's review in
one combined concept round (round 36) at the end of M5.** The most important one changes an approved
spec: **H1 = a drops the approved 15 px/s creep of boss phase 1** (see *Layout* and *Decisions*); the
user must confirm that. Step H4 added two more for review: **the root field scrolls at 80 px/s** (not
140) so the root-burst growths stand on the screen once they fire, and **the Spire's paid bounty is
26.8 % of the budget**, not 30 % (see *Credit budget*).

## Briefing

Two pages, as Levels 08–11, each **one screen** (`BriefingLayoutTest`): Okafor's mission (the
Spire and the way in), then Varga's page on the roots and what to bring. The text of the approved
single page is split between the two, with the unchanged sentences kept. Draft texts, for review
in concept round 36:

<!-- data: briefing -->
> **Commander Okafor:** "Lancer, Aegis Actual. Something grew out of Geneva Concord two nights
> ago: the Siege Spire, three kilometres tall and still growing. Your flight goes in first,
> along the lakefront and through the old town to the rotunda. Destroy it. Aegis Actual out."
>
> **Dr. Varga:** "The Vrell fight as one, and I believe the Spire binds them. Its roots run
> under the old town and grow guns where they break the surface. Kill the roots and the trunk
> starves. Bring anti-ground and something that pierces. When the crown opens, look behind you."
<!-- /data -->

*Images* (production art straight away, H8 = a; [briefing images](../../../ui/briefing/README.md)):
Okafor's page `level-14-spire-over-geneva` (the Spire over Geneva seen from the lake, the canopy
over the old town), Varga's `level-14-root-scan` (her scan of the root grid under the old town,
the burst points marked); the level's data names them as each page's `image` once step H5 has
rendered them.

*Hangar teaser* (shop screen before L14; Rook, `LT. K. TANAKA / AEGIS TWO`, text only, not
voiced, as Levels 10 and 11; the approved unattributed text is given to him, shortened in step H4
to fit the intel panel, `IntelPanelLayoutTest`, as Varga's L2 and L3 lines below):

<!-- data: teaser -->
> **Rook:** "Roots first, flyers from behind, then the Spire. I'll take the six o'clock."
<!-- /data -->

*Varga's intel line* per sensor-suite level (the data's `varga`; draft texts for round 36; fitting
the intel panel is checked by `IntelPanelLayoutTest`, which may force shorter lines as it did for
Level 11; the approved single line is the L3 line):

| Sensor suite | Line |
|---|---|
| none | "Geneva Concord, Lancer. Something has been growing here for two nights, on the ground and in the air." |
| L1 | "Mostly from ahead. Roots break the streets: watch for the dust." |
| L2 | "Root guns, hive nodes, cloaked Wraiths. And the Spire." |
| L3 | "Bring anti-ground. When the crown opens, look behind you." |

## Threat profile

Rendered from the level data (the intel's rule counts each wave's `from`); the draft's "front 71 /
rear 29" counted a different wave list. The maw's Wraiths loop to the rear in the fight but are not
edge waves, so they are not in the numbers.

<!-- data: threat-profile -->
| Field | Value |
|---|---|
| Dominant layers | `ground` (the roots, the root-burst growths, the hive nodes, Creepers, Ravagers), `air`, `high-air` (cloaked Wraiths under the canopy) |
| Attack directions | front 88% · sides 8% · rear 4% (the approach by its waves; in the boss fight the maw's Wraiths loop round to the rear) |
| Density | 4 |
| Recommended traits | `anti-ground`, `piercing`; `rear` helps against the Wraiths (three traits do not fit the intel panel) |
| Hazards | Root bursts (telegraphed by dust, ground layer, no collision); low light under the canopy |
| Boss / mid-boss | Act boss [Siege Spire](../../../enemies/bosses/siege-spire/README.md) |
| Sensor-suite detail | **none**: Geneva Concord, `ground` + `air` + `high-air`, front. **L1**: + the directions, density 4, hazard "root bursts", the objective `DESTROY THE SIEGE SPIRE`. **L2**: + Creeper, Ravager, Hive Node, Wraith, Needler, Skitter, Spine Turret, Polyp Mortar portraits; the boss's name and silhouette (`siege-spire`). **L3**: + `anti-ground` and `piercing` highlighted; the timeline strip marks the root field (60–95 s) and the boss (108 s); 1 secret. |
<!-- /data -->

## Objective

- **Primary** `reach-end` with the arena as the condition, as Level 07: the scroll halts at the
  arena and the level ends when the Siege Spire is destroyed and the daylight section has run.
  The briefing and the objective line read `DESTROY THE SIEGE SPIRE` (the boss's name on the
  bar). It is not a `boss` primary.
- **Secondary** "Roots" (`groups`): destroy all **8 root-burst growths** (4 Spine Turrets and 4
  Polyp Mortars; **easy 4**, Spine Turrets only) before they leave the screen. A growth counts
  from its placement; "leaving the screen" is the bottom edge, as for any ground unit. The
  tracker reads `ROOTS 3/8` (easy `n/4`). Pays **63** credits in Act 1 terms (101 in Act 2, the
  credit factor 1.6); the typical haul counts it at the economy's secondary share. Okafor's
  line plays when it is met.

## Layout

Approach at **140 px/s** (the chosen Geneva scene), **108 s** of it, with **the root field at
80 px/s** (step H4, pending the user's review: a root-burst growth appears 1 s after its dust and
fired 1.5 s later (0.75 s since step H7b), and at 140 px/s it would by then have scrolled 350 px from its burst, out of the
player's reach before the bottom edge; at 80 px/s it stands about 3 s on the screen once it fires).
The **boss arrives at t=108**: the arena section starts then, with speed 0, and the boss warning, the
boss's arrival (its bar, the sting, track 22) and the boss checkpoint are one moment. **The 15 px/s
creep of boss phase 1 in the approved spec is dropped** (H1 = a, pending the user's review): the
built anchored arena needs speed 0, and all six roots stand on the screen from the start. "In turn"
comes from the staggered fire (pods 1.6 s apart, the mortar roots every 4 s) and from the player's
choice of root: the roots' guns turn toward the ship, so the player's x decides the exposure. The
arena runs from t=108 to t=250; the level clock halts at the arena's end while the Spire lives, and a
kill jumps the clock to it (Level 07's `arenaJump`), the scroll ramping up over 1 s into the 12 s
daylight section, which **drifts at 25 px/s** (steps H7b and H9, pending the user's review: at 140 px/s the
stump scrolled off about 4 s after the kill, before the light fell on it, at 40 px/s about 9 s after it,
10 s before the debrief; now it stays on the screen through the daylight's beams until past the section's
end, the scroll running on to the debrief). Total 262 s on the level clock and 13,320 px of ground (the scroll column
below). Motion budget: the strong elements are the street traffic and lamps' flicker in sections 1–2
(abandoned cars standing still, only a few moving) and the root bursts and dust in section 3. The
canopy shadow drifts slowly. In the arena everything moving drives off real seconds, not the halted
scroll.

<!-- data: level-sections -->
| Section | t (s) | Scroll (px) | Speed (px/s) | Atmosphere | Layers and content | Purpose |
|---|---|---|---|---|---|---|
| 1. Lakefront | 0–20 | 0–2,800 | 140 | light | `deep`: Lake Geneva at dusk, dark water. `ground`: the lakefront quays, the Jet d'Eau plume (leaning away from the screen centre, perspective rule), the Rhône bridges, a CDF armoured car half under creep on the Pont du Mont-Blanc (the secret). `low-air`: Hammer flight bombers passing below (scenery). `high-air`: the canopy's edge as a soft shadow (≤ 40 %). | Set-up; light popcorn from t=7. |
| 2. Old Town | 20–60 | 2,800–8,400 | 140 | light | `ground`: perimeter blocks of row houses with ridged roofs and courtyards, the cathedral towers (leaning, perspective rule), sodium-lit streets, parks and tree rows, creep veins along the gutters, the Place du Molard with two hive nodes. `high-air`: canopy shadow. | Creepers in the streets, a turret nest on the cathedral square, the hive-node pair. |
| 3. Root Field | 60–95 | 8,400–11,200 | 80 | medium | `ground`: streets broken by roots as thick as trams; four root bursts split the paving (1 s dust telegraph each) and grow a Spine Turret and a Polyp Mortar; creep over the roofs. `low-air`: spore-haze banks lit violet from below. | The signature set piece: roots growing guns. Ravagers out of a crater; the first Wraiths from behind. |
| 4. Concord Approach | 95–108 | 11,200–13,020 | 140 | medium | `ground`: avenues converging on the Concord rotunda's roundabout; the Spire's trunk rising out of it, leaning away from the centre; roots radiating along the avenues. `high-air`: the canopy directly overhead. | Last Wraiths; the Choir's song; the trunk grows on the horizon. |
| 5. Siege Spire | 108–250 | 13,020–13,020 | 0 | clear | `ground`: the arena around the rotunda, the trunk at the top, four turret-pod roots and two mortar roots spread along the avenues, all on the screen. Phase 3 the stump and the ground debris after the tear-free shockwave. `high-air`: canopy shadow only. | The boss fight (≈ 135 s at medium); the warning, the arrival and the checkpoint at t=108. |
| 6. Daylight | 250–262 | 13,020–13,320 | 25 | clear | `ground`: no enemies, no boss. The canopy withers and tears; low sunlight falls across the old town, the lake and the stump. | Release; act complete. |
<!-- /data -->

## Waves

Enemy definitions are linked per row; formation names come from the
[formation vocabulary](../../../enemies/README.md#formation-vocabulary). Returning units use the
act HP factor from the [balancing basis](../../../enemies/README.md#balancing-basis) (the Creeper
40; Hive Node, Ravager and Wraith at their L14 values; none for the Siege Spire, whose first
level this is, nor for the small units); HP values live in the enemy specs only.

**Density and pacing.** The draft had no enemies before t=24 and 42 units up to the arena. The
level has popcorn (Skitters, Needlers) from t=7, a wave every 3–3.5 s, from the front and the sides,
and the two rear ambushes as authored. The introduction of the root bursts keeps a quiet window: no
wave enters from t=62 until t=67 (the first burst's dust at 63). At medium **112 units** reach the
field before the arena (95 in waves, 17 ground units with the growths; easy 81, hard 142), **67.6 a
minute** from the launch to the arena (`DensityTest`, at least 50), and the screen is never empty for
long (`PacingTest`). The t=82 Wraiths enter from the rear on medium and hard; on easy a Needler V-wing
from the front replaces them (a Wraith flies only the rear ambush).

<!-- data: waves -->
| t (s) | Section | Formation | Enemies (link) | Count | Enter from | Notes |
|---|---|---|---|---|---|---|
| 7 | 1 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 3 | front (right) | Popcorn over the lake |
| 10.5 | 1 | line abreast | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front | Popcorn |
| 14 | 1 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 3 | front | Over the bridges |
| 16.5 | 1 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | sides (alternating edges) | Popcorn from the sides |
| 21 | 2 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 5 | front | Over the rooftops |
| 24.5 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 3 | front (left) | |
| 28 | 2 | convoy | [Creeper](../../../enemies/ground/creeper/README.md) | 3 | front | Along the Rue du Rhône |
| 31 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 3 | front (alternating edges) | |
| 34 | 2 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 4 | front | |
| 37.5 | 2 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 5 | front (left) | Weaves between the cathedral towers |
| 41 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 3 | front (right) | Over the cathedral square's turret nest |
| 44.5 | 2 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 3 | front | |
| 47.5 | 2 | convoy | [Creeper](../../../enemies/ground/creeper/README.md) | 3 | front | Down from the cathedral hill |
| 51 | 2 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 5 | front (right) | |
| 54.5 | 2 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 3 | front | Over the Place du Molard |
| 59 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 3 | front (alternating edges) | Gone before the first root burst |
| 67 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 3 | front (left) | After the first burst's quiet window |
| 69.5 | 3 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 3 | front | |
| 72 | 3 | pack | [Ravager](../../../enemies/ground/ravager/README.md) | 4 | front | Out of a root-burst crater; the leader drops an overdrive |
| 76 | 3 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 5 | front (right) | |
| 79.5 | 3 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 3 | front | |
| 82 | 3 | rear ambush | [Wraith](../../../enemies/air/wraith/README.md) | 2 | rear | Cloaked on `high-air` under the canopy; Rook's rear bark (not scripted); easy: a Needler V-wing from the front instead |
| 82 | 3 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 3 | front | Easy only: instead of the rear Wraiths |
| 85.5 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 3 | front (alternating edges) | |
| 88.5 | 3 | line abreast | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front | |
| 91.5 | 3 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 3 | front | |
| 95.5 | 4 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | sides (alternating edges) | Popcorn from the sides on the avenues |
| 99 | 4 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 3 | front | |
| 102 | 4 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 3 | front (alternating edges) | |
| 104 | 4 | rear ambush | [Wraith](../../../enemies/air/wraith/README.md) | 2 | rear | The last wave before the Spire |
| 108 | 5 | boss | [Siege Spire](../../../enemies/bosses/siege-spire/README.md) | 1 | front (anchored at the rotunda, scrolls in with the ground) | At the halt (the warning, the bar and the boss checkpoint); phase 2: the maw launches 2 Wraiths every 8 s (hard 3) as ambushes that loop to the rear |

Totals: Skitter 55 · Needler 30 · Creeper 6 · Ravager 4 · Wraith 4 · Siege Spire 1.
<!-- /data -->

The maw's launches in the fight (2 Wraiths every 8 s, hard 3; three launches in the measured medium
fight) are ambush plans, not edge waves: the bottom-edge warning and Rook's Trail formation apply
(the boss row's notes).

## Ground targets

<!-- data: ground-targets -->
| Section | Target | Effect |
|---|---|---|
| 1 | A CDF armoured car half under creep on the Pont du Mont-Blanc (trigger, t≈15) | 6 hits clear the creep; the cargo hatch pops open as the hidden crate |
| 2 | [Spine Turret](../../../enemies/ground/spine-turret/README.md) turret nest ×3 on the cathedral square (t≈38) | Bounty |
| 2 | [Hive Node](../../../enemies/ground/hive-node/README.md) ×2 in the Place du Molard, hardened (t≈55) | Optional: each releases 2 Skitters while alive (the budget counts one release each) |
| 2 | Spine Turret nest ×3 beside the hive nodes (t≈55), **hard only**; hard ×3 | Bounty |
| 3 | Root bursts ×4 (t≈63, 74, 84, 92), each growing [Spine Turret](../../../enemies/ground/spine-turret/README.md) + [Polyp Mortar](../../../enemies/ground/polyp-mortar/README.md) after a 1 s dust telegraph; easy Spine Turret | Invulnerable and silent for 0.75 s (hard 0.5 s); the secondary's group `roots` |
<!-- /data -->

## Hazards

- **Root bursts** (H2 = a, data-driven `root_bursts`: a time, a position, the growths per
  difficulty, telegraph 1.0 s, first-shot delay 0.75 s, hard 0.5 s (step H7b); the bursts at t=63 (x 150, 120 px
  below the top edge), 74 (x 330, 110 px), 84 (x 170, 140 px) and 92 (x 320, 100 px), each growth
  26 px either side of its burst's point, the Spine Turret on the left): the paving cracks and dust
  rises for 1 s, then a root breaks out and grows a Spine Turret and a Polyp Mortar on the ground
  layer. No collision. A growth takes **no damage and fires nothing while it grows**, then fires
  its first shot **0.75 s after it has appeared** (hard 0.5 s; step H7b, pending the user's review: at
1.5 s the growths fired from about 220 px above the bottom edge, on top of the ship; now about
280 px). From then on it scrolls with the
  ground. A burst lies **100–400 px below the top edge** and never within **60 px of the ship's
  x** at the telegraph. The units count among the level's enemies from the burst, and in the
  secondary's group `roots` from their placement. The crater at the Ravager pack's entry (t=72) is
  a burst without growths (a backdrop and dust effect at the first burst's x; the pack itself
  enters at the top as usual). Easy: the growths are Spine Turrets only.
- **Low light**: the canopy darkens the old town (backgrounds at the dark end of the recede
  range). Enemy accents and bullets keep their full brightness.

## Secrets and pickups

- **Armoured car** (t≈18): six hits clear the creep off the car on the bridge, and its cargo
  hatch pops open with a CDF supply crate of **88** credits in Act 1 terms (**141** in Act 2; the
  draft's "140" was the Act 2 amount written into the data).
- **Overdrive** ×2: from the t=72 Ravager leader, and from the maw when it dies (phase 2 → 3).
- **Armour patches** ×2: a CDF supply drone (Level 13's, [allies](../../../allies/README.md)) flies
  in from the bottom edge at t≈96, releases its patch at t=100 at x 240, 380 px above the bottom
  edge (the data's `drops`, as Level 13's) and leaves the same way (step H9, pending the user's review;
  it was a placed pickup that simply entered at the top edge), and the tear-free shockwave throws one
  up from the debris at the start of phase 3.

## Radio chatter

Spoken radio ([voice](../../../audio/voice/README.md)), as Levels 08–11, with Rook's lines
`requires: escort`. The draft's script is **retimed to the HUD's 1 s rule** (`RadioTimelineTest`,
with the voiced lengths of step H6): the draft's clash at t=106/107/108 on top of the boss
warning is gone, with the Choir at t=96 and Varga at t=102; **Rook's t=108 line "Here comes the
big one" is the boss-warning bark** (his AI says it at the warning, so the scripted copy is
dropped), and so is the draft's t=80 "Six o'clock, Lancer!" (his rear bark covers the t=82 and
t=104 ambushes). Okafor's opening line holds the radio for about 12 s, so Hammer Lead speaks at
t=13 (his flight passes at t=16), Rook at t=21 and Varga's canopy line at t=31.5. The arena's lines are **events** (the
level clock is halted): `boss-phase` (phases 2 and 3), `boss-destroyed` and
`secondary-objective`; a phase's lines queue as `PHASE` lines (step H7b): they wait for a gap like
event lines but go stale only after 20 s, so Rook's phase-3 line, queued after Okafor's, plays (at
the 6 s of an event line it went stale behind it); a phase line still waiting at the kill is dropped.
The secondary's line, a closing line, **goes ahead of the Choir** when the roots die before it
(about t=93.5 in a normal run; step H9, pending the user's review): queued while the radio is free, it
opens at once and the Choir's and Varga's timed lines wait for it (about 8 s later, Varga's then over
the Spire's arrival). When the roots die late, while a line plays, it waits for a gap as before and
**once the boss warning starts it is dropped** (or cut if it opened less than 1.5 s before), since the HUD
shows the objective done, and Rook's boss-warning bark plays instead of going stale behind it (step
H7b, pending the user's review). The closing line is queued as `CLOSING` so that the daylight section
and the debrief wait for it.
The Choir's text line is voiced as in the earlier levels (the portrait frame full of static).

<!-- data: radio -->
| Trigger | Speaker | Line |
|---|---|---|
| t=1 | Okafor | "All units, Aegis Actual. Everything the Coalition has left is in the air over Geneva tonight. Make it count." |
| t=13 | Generic CDF (Hammer Lead) | "Hammer flight on station. Call your targets, Aegis." |
| t=21 | Rook | "Lancer, whatever happens in there, I'm on your wing. As usual." |
| t=31.5 | Varga | "The canopy is one organism. The roots feed the trunk. Kill the roots and the trunk starves." |
| t=54 | Varga | "Hive nodes in the Molard. Hardened, like Lagos." |
| t=62 | Okafor | "Roots surfacing in the streets! They're growing guns." |
| t=96 | The Choir (distorted) | "[the Choir sings]" |
| t=102 | Varga | "It's singing to the whole city. Lancer, that's the Spire." |
| Phase 2 (`boss-phase` Maw) | Varga | "The crown is opening. It's launching flyers, and they'll loop behind you." |
| Phase 3 (`boss-phase` Free spire) | Okafor | "It's tearing loose! Stay on it, Lancer!" |
| Phase 3, queued after Okafor's line | Rook | "Roots and all. Somebody call a gardener." |
| Spire destroyed (`boss-destroyed`) | Varga | "The canopy's dying. All of it, all at once." |
| Secondary objective met | Okafor | "Root grid in the old town destroyed. The engineers will thank you." |
| Level end (the closing line) | Okafor | "Every Vrell on Earth just lost its voice. They're scattering. We take the fight to them now." |
<!-- /data -->

Rook's phase-3 line is a second `boss-phase` cue for the same phase: it is queued after Okafor's
(no event delay exists; step H4, pending review; it waits as a `PHASE` line, step H7b). The car's
line plays when its secret is found.

## Boss / mid-boss

Act boss: [Siege Spire](../../../enemies/bosses/siege-spire/README.md). Its phases, HP and
attacks live in the spec. Arena notes for this level:

- The arena starts at t=108 with the boss warning, the boss bar and the checkpoint in one
  moment: the warning (track 22) plays on arrival, and the boss theme starts after it. The
  trunk stands in the Concord rotunda at the top of the arena; the four turret-pod roots and
  two mortar roots break out along the avenues radiating from it, **all six on the screen from
  the start** (no creep, H1 = a).
- Phase 2: the maw's Wraiths come at the player from the bottom edge as ambushes (H3 = a): the
  sim adds the 3 s bottom-edge warning before each re-entry, Rook switches to his Trail formation
  and calls each launch (bark priority 2), at most 4 alive at once.
- Phase 3: the freed spire lifts off its anchor over ≈ 3 s, rises to `air` and drifts across the
  upper half (H4 = a); the withered stump and the shockwave debris stay on the ground layer as
  scenery, with no collision (the stump is drawn from the boss's state, not as a backdrop
  piece).
- The Airstrike can hit the roots and the maw (ground targets), capped at 150 per part per
  strike by the [specials](../../../player/specials/README.md) rules.
- The boss checkpoint is recorded at the arrival, t=108, on easy and medium, per the
  [retry](../../../systems/retry/README.md) rules; there is none on hard.
- Bounty: **452 as paid** (26.8 % of the budget; 372 in Act 1 terms: pods and mortar roots 31 each,
  maw 62, core 124 at the spec's ratios 60:60:120:240), see *Credit budget*. The maw's Wraiths pay
  their own bounty.
- The fight's length is **measured** (H10 = a; the [Siege Spire](../../../enemies/bosses/siege-spire/README.md#stat-block)):
  the autopilot's fights on the plan's fit over five seeds, the HP ×1.8 the spec's, a mean of 129.5 s
  at medium (every seed 126.1–131.7 s; remeasured in step H6 after pod 1's offset fix), the par 130 s. Flown in this level (seed 2185) the fight
  lasted 97 s on easy, 136 s on medium and 204 s on hard.

## Music & ambience

The Act 2 B theme "Firestorm" (track 7 in the [track list](../../../audio/music/README.md#track-list))
for the approach: the base stem from section 2, the full mix from section 4. At the arena start,
t=108, the boss warning (track 22) plays and the Vrell boss theme "The Choir Descends" (track 18)
starts after it and runs through the fight, fading at the kill. The daylight section has **no
music**, and there is no mid-level jingle. After the kill the debrief plays the `mission-complete`
jingle as every level does, and the act complete fanfare (track 24) plays under the first outro
page (H6 = a, the Act 1 order), **not over the daylight section**. Ambience: the
[megacity ambience](../../../audio/sfx/README.md#ambience-per-setting) with the sirens replaced by
a low organic hum from the canopy, which cuts out when the Spire dies.

## Act end

Level 14 ends Act 2 and follows the order of Act 1's end (M4, H6 = a): the Spire's death →
the **daylight section** (12 s: the canopy withers and tears on a real-seconds timeline from the
boss's death, low sunlight, the closing radio line; no enemies, no music, the ambience without
the hum) → the **debrief with the act summary** (`mission-complete` jingle, the tally, the summary
`ACT II COMPLETE · HOMEFRONT`) → the **act outro**, four voiced pages (H7 = a; the data is
in the [Act 2 README](../README.md)) with `act-complete` under page 1 → the hangar. There is no
Level 15 yet: after the outro the hangar reads `BEFORE L15 - NOT BUILT YET` and cannot launch
(H9 = a), as Act 1's did in M4; M6 turns the end of Act 2 into the release's end (credits, high
scores). `--act-end` works unchanged for this level.

## Credit budget

Budget(14) = 700 × 1.07¹³ ≈ **1,687**, the typical haul's target
([economy](../../../systems/economy/README.md#per-level-budget)), not a perfect collection. The
draft's 2,410 was the old perfect-run curve with Act-2-scaled amounts. **The data keeps Act 1 terms**
(Skitter 5, Needler 12, Creeper 22, Ravager 18, Wraith 30, Hive Node 45, Spine Turret 12, Polyp
Mortar 15, the car's crate 88, the secondary 63); the code pays each × the act factor 1.6, and
bounties also × the level's **`bounty_scale` 0.76**, which puts the typical haul on the budget within
±5 % (`TypicalHaulTest`). The maw's Wraiths are budgeted at the measured fight's count (three launches
of 2, the boss's `spawns`), the node-spawned Skitters at one release per node.

**The Spire's share (step H4, pending the user's review): 452 paid = 26.8 % of the budget**, not the
planned ≈ 506 (30 %). The typical haul sits on the budget and a perfect run must earn at least 1.4 ×
it (`TypicalHaulTest`): the boss pays the same in both, so every credit it takes off the scaled kills
narrows the gap between them; at 30 % the perfect run falls short (≈ 1.39 ×), at 26.8 % it reaches
1.42 ×. The act boss's share stays inside its tolerance (20–40 %, `BalanceTest`).

<!-- data: credit-budget -->
| Source | Perfect run | Typical haul |
|---|---|---|
| Kills: Skitter 55 × 5 + Needler 30 × 12 + Creeper 6 × 22 + Ravager 4 × 18 + Wraith 4 × 30 + released Skitter 4 × 5 | 1,198 | 769 |
| Ground targets: Spine Turret 7 × 12 + Hive Node 2 × 45 + Polyp Mortar 4 × 15 | 287 | 230 |
| Boss: Siege Spire (pods 4 × 31 + mortars 2 × 31 + maw 62 + core 124) | 454 | 454 |
| Boss spawns: Wraith 6 × 30 (a typical fight) | 216 | 130 |
| Secret: armoured car (hidden crate, 5% of budget) | 141 | 70 |
| Secondary: destroy every root-burst growth | 101 | 50 |
| **Total** (bounty scale 0.76) | **2,397** | **1,703** |
| Budget(n) = the typical haul's target; typical +1 %, perfect 1.42 × budget | | 1,687 |
<!-- /data -->

## Difficulty notes

- **Easy**: the root-burst growths are Spine Turrets only (the secondary counts 4); a Needler V-wing
  from the front replaces the t=82 Wraiths (a Wraith flies only the rear ambush); the boss ×0.75.
- **Hard**: a second Spine Turret nest beside the hive nodes; root bursts grow 1.0 s after the
  telegraph instead of 1.5 s; the boss ×1.3 and its hard-mode hooks from the spec (3 Wraiths per
  launch, a 180° sweep, a ring of 12 for the lob). There is no boss checkpoint on hard.

## Concept art

The look comes from the chosen Geneva scene in [art direction](../../../art-direction/README.md):
[scene-geneva-r10-a.png](../../../art-direction/concept/scene-geneva-r10-a.png) and its
[scroll loop](../../../art-direction/concept/scene-geneva-r10-a.gif); the boss follows its chosen
[Siege Spire](../../../enemies/bosses/siege-spire/README.md) concept. Concept round 36 (part H, H8 = a,
pending the user's review) auditions what has no concept: the **root burst** and the **daylight** a/b
here, the **tear-free roar** and the **canopy withering** a/b in the [sfx](../../../audio/sfx/README.md)
round; the armoured car, the outro images, the burst's sounds and the hum get one version; the Geneva
backdrop and the Spire go straight to production. Generator notes: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/root-burst-r36-a.png](concept/root-burst-r36-a.png), [.gif](concept/root-burst-r36-a.gif) | Root burst, look a "breaking paving": cracks with a violet glow seeping through, dust, a slate root arching out with two knots, the growths swelling out of them (`tools/art/root_burst.py`; in the game until the pick: `--assets` wrote `assets/sprites/root-burst-decal`, `-puff`, `-root`, step H5) | proposed |
| [concept/root-burst-r36-b.png](concept/root-burst-r36-b.png), [.gif](concept/root-burst-r36-b.gif) | Root burst, look b "creep surge": creep tendrils from the gutters, spore mist, a sac mound swelling and two buds splitting open | proposed |
| [concept/daylight-r36-a.png](concept/daylight-r36-a.png) | Daylight section, look a "golden hour": warm grade, low-sun beams through the tearing canopy (`tools/art/daylight_l14.py`; still and timeline) | proposed |
| [concept/daylight-r36-b.png](concept/daylight-r36-b.png) | Daylight section, look b "pale dawn": cool grade, haze burning off from the lake, no beams | proposed |
| [concept/backdrop-final-r36-a.png](concept/backdrop-final-r36-a.png), [.gif](concept/backdrop-final-r36-a.gif) | Production backdrop straight from scene-geneva-r10-a (`tools/art/backdrop_l14.py`, notes in [concept/prompts.md](concept/prompts.md#backdrop-final-r36-a); its block for the level data, for the main agent to merge, is [concept/backdrop-proposal.yaml](concept/backdrop-proposal.yaml)): the city rebuilt from plans (perimeter blocks, tile and slate roofs, sodium lamps, the canopy's mottled shadow baked in): the lakefront with the lake, the old town, the root field with roots as thick as trams, cracked paving and collapsed houses, the daylight tiles; sections 4–6 one unique strip (the converging avenues, the Concord plaza under the Spire's anchor with six avenues along its roots, the daylight graded by its distance past the arena frame); the Rhone with the Pont du Mont-Blanc, Saint-Pierre's square and the Place du Molard cut from the tiles under them, the cathedral towers in perspective, the Jet d'Eau, the Ravagers' crater, boulevards over the tile seams, Hammer flight and the few moving cars; the spore haze and the canopy's veins; the game's daylight looks (the canopy tearing, the beams, look a); composites with stand-ins for the units and the Spire; the GIF the ease into the halt and the daylight after a kill | proposed |
| [concept/l14-props-final-r36-a.png](concept/l14-props-final-r36-a.png), [.gif](concept/l14-props-final-r36-a.gif) | Production art (`tools/art/l14_props.py`): the CDF armoured car under creep, cleared hit by hit, hatch open with the supply crate | proposed |
| [concept/act-2-outro-final-r36-a.png](concept/act-2-outro-final-r36-a.png) | Production art (`tools/art/outro_l14_images.py`): the Act 2 outro's four briefing images, the stump in Geneva at sunrise, the memorial wall of names at dawn, Ares Landing's priority signal from Mars, Aegis Wing and the Mars transport on the apron at dusk | proposed |
| [concept/level-14-capture-final-r36-a.png](concept/level-14-capture-final-r36-a.png), [.mp4](concept/level-14-capture-final-r36-a.mp4) | Capture of the game (medium, a strong fit with Rook, **`--invulnerable`**: the dead-reckoning driver cannot survive the Spire on the debug start's cores): 115 / 117 kills, ROOTS 8 / 8, secret 1 / 1, the Spire in 1:26 (par 2:10), A+; nineteen frames (the briefing pages and the hangar from a won Level 13, one phase-1 frame from a run without `--invulnerable`) and the whole run with sound through the act end ([recipe](concept/prompts.md#level-14-capture-final-r36-a)) | proposed |
| [concept/level-14-capture-fixes-r36-a.png](concept/level-14-capture-fixes-r36-a.png), [.mp4](concept/level-14-capture-fixes-r36-a.mp4) | Supplementary capture of the step H7b fixes (step H8; the level time burnt in): Hammer flight at x 130 clear of the car, the growths' first shot about 290 px above the bottom edge, the radio at the roots' end and the boss arrival (Rook's warning bark, his phase-3 line), the free spire over the ash stump, the withering 3 s after the kill, the daylight's 40 px/s drift; the bottom row a run **without `--invulnerable`** on the balance plan's L14 fit (cores included) with Rook, lost five times in the Spire fight ([recipe](concept/prompts.md#level-14-capture-fixes-r36-a)) | proposed |
| [concept/level-14-capture-daylight-r36-a.png](concept/level-14-capture-daylight-r36-a.png), [.mp4](concept/level-14-capture-daylight-r36-a.mp4) | Supplementary clip of the step H9 changes (step CF; 47 s with sound, the level time burnt in, and a sheet; `--invulnerable`): Okafor's root-grid line at the roots' end (≈ 95) ahead of the Choir (≈ 105) and Varga (≈ 111), the CDF supply drone carrying the armour patch in from the bottom edge (t 96.5–103), the kill (≈ 194.5) and the daylight at 25 px/s: the stump on the screen through the beams, its last root tips leaving at ≈ 215.3 (≈ 21 s after the kill), the debrief at ≈ 217.5 ([recipe](concept/prompts.md#level-14-capture-daylight-r36-a)) | proposed |
| [concept/act-2-outro-capture-r36-a.png](concept/act-2-outro-capture-r36-a.png) | Capture of the act end from the same run (a real kill): the daylight and the closing line, the debrief, the act summary, the four voiced outro pages, the hangar before L15 ([recipe](concept/prompts.md#act-2-outro-capture-r36-a)) | proposed |

## Implementation

- [x] README reworked to the decided design: arena start = warning = arrival = checkpoint at
      t=108, no creep, density, budget(14), the act-end order (M5 part H, step H1a)
- [x] Level data `data.yaml` with every block below and this README's marked tables rendered
      from it; `PartHLevelKeysTest`, `Level14Test` (M5 part H, step H4)
- [x] Scroll timeline, sections, atmosphere intensity and parallax content per layer as in
      *Layout*; the arena section with `speed: 0` halting the scroll at t=108, the 12 s daylight
      section after it, primary `reach-end` (M5 part H, step H4)
- [ ] Production backdrop `tools/art/backdrop_l14.py`: six sections, the canopy shadow and spore
      haze, the rotunda arena, the daylight grade; moving scenery on real seconds in the arena
      (M5 part H, step H5)
- [x] Root bursts (H2 = a): `root_bursts` with the placed ground unit, the 1 s telegraph, the
      invulnerable silent growth, the first shot after 0.75 s (hard 0.5 s; step H7b), the position rule,
      counted in the density and the secondary (M5 part H, steps H3 and H4)
- [x] Root-burst look: the cracking and dust, the root breaking out, the grow-in, the crater;
      look a in the game until the user's pick in round 36 (M5 part H, steps H5 and H6:
      `RootBurstLooks`, `Level14LooksTest`)
- [x] Wave script matches the *Waves* table: at least 50 enemies a minute at medium from t=8 to
      the arena (`DensityTest`), no empty screen over 3 s on every difficulty (`PacingTest`)
      (M5 part H, step H4)
- [x] Boss arena per the arena notes: all six roots from the start, the maw's ambush launches
      with the dynamic bottom-edge warning and Rook's Trail, the lift-off, the stump, the
      checkpoint at t=108 on easy and medium and none on hard (`Level14Test`) (M5 part H, steps H3
      and H4)
- [x] The fight measured on the plan's fit (five seeds, every seed 90–180 s, the mean 129.5 s, the par 130 s),
      the HP tuned, the par and the bounty set (`SiegeSpireTest`, `BalanceTest`) (M5 part H,
      step H4)
- [x] Armoured-car secret (6 hits, crate 88 in Act 1 terms; its creep cleared blob by blob, a
      frame a hit, then the hatch open), overdrives, armour patches (the drone's patch as a placed
      pickup without a drone sprite; since step H9 carried in by Level 13's supply drone from the bottom
      edge, `drops`) (M5 part H, steps H4, H5, H6 and H9)
- [x] Radio chatter cues incl. the boss phase events, voiced, every timed line at most 1 s late
      (`RadioTimelineTest`, with Rook and without him); the closing line queued `CLOSING` (M5
      part H, steps H4 and H6)
- [x] Secondary objective `ROOTS n/8` (easy `n/4`) tracked and rewarded 63 in Act 1 terms, the
      debrief's row `ROOTS n / 8` (M5 part H, steps H3, H4 and H6; `PartHHudTest`,
      `LevelScreenPartHTest`)
- [ ] Threat profile from the data, Varga's four intel lines and Rook's teaser fitting the intel
      panel (`IntelPanelLayoutTest`), the Spire's intel silhouette and the L3 strip's marks (M5
      part H, steps H4 and H5)
- [x] Briefing: Okafor's and Varga's pages, each one screen (`BriefingLayoutTest`), with their
      images (M5 part H, steps H4 and H5; checked in step H6)
- [x] Daylight section: no enemies and no music, the canopy withering and tearing from the boss's
      death, the sunlit grade; daylight look a in the game until the user's pick in round 36 (M5
      part H, steps H5 and H6: `SpireLooks`, `Level14LooksTest`)
- [x] Act end: the debrief with the act summary, then the four outro pages with `act-complete`,
      then the hangar (`--level 14 --act-end`; `OutroOnwardTest`, `DebriefActEndTest`,
      `CampaignRouteTest`) (M5 part H, steps H4 and H6)
- [x] Typical haul at medium within ±5 % of budget(14) = 1,687 with the level's `bounty_scale`
      (`TypicalHaulTest`); the Spire's paid share ≈ 30 % (`BalanceTest`) (M5 part H, step H4)
- [x] Music: "Firestorm" base from section 2, the full mix from section 4, the warning at the
      arena start, the boss theme after it, none in the daylight; the canopy hum and its cut
      (M5 part H, step H6; `Level14SoundsTest`)
- [x] Easy/hard variations as in *Difficulty notes* (M5 part H, step H4)
- [x] Captures for concept round 36: the approach, a burst, the Wraiths' rear entries, the three
      phases, the lift-off, the death and the daylight, the act end (M5 part H, step H7)

## Open questions

- None open. All of H1–H10 are decided as recommended **while the user is on vacation** and wait
  for the user's review in the combined round 36 at the end of M5 (see *Decisions*): above all
  that the approved 15 px/s creep is dropped (H1) and that all six roots are in range at once.

## Decisions

- 2026-10-01: Promoted from the Act 2 roster to a draft level document.
- 2026-10-01: Open question resolved: the maw's Wraiths pay their full Wraith bounty at the Act 2 factor, as budgeted here (user decision; recorded in the Siege Spire spec).
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-11: M5 part H, step H1a: the approved first draft reworked to the design the gap
  analysis decided. **The user is on vacation: H1–H10 are all the recommendation (a) and all the
  stated defaults apply, pending the user's review in one combined round (round 36) at the end of
  M5.** **H1 = a (recommended; pending the user's review): the approved 15 px/s creep of boss
  phase 1 is dropped, a change to an approved spec that the user must confirm**: the arena is a
  halt (speed 0), all six roots on the screen from the start, "in turn" from the staggered fire
  and the player's choice of root; rejected: b (an arena of speed 15 in phase 1 and 0 afterwards: a
  phase-keyed scroll, a changed `haltScroll`, the backdrop's boss pieces and the daylight offset
  all move with it) and c (a creep through the whole arena: breaks the anchor, the phases and the
  stump). **H2 = a** data-driven root bursts (`root_bursts`; invulnerable silent growths; 100–400
  px below the top edge, not within 60 px of the ship's x; no collision); rejected: b (growths
  enter at the top: the set piece loses its point) and c (bursts near the ship: harder to read).
  **H3 = a** the maw's Wraiths are ambush launches with a dynamic 3 s bottom-edge warning, Rook's
  Trail formation and at most 4 alive; rejected: b (straight launches: no rear attack) and c (no
  warning: against the act's rule). **H4 = a** one boss and one bar: a phase `move` lifts the whole
  boss over ≈ 3 s (invulnerable), the core on `air`, one armour patch at the start, the stump drawn
  from the boss's state; rejected: b (a second set piece for phase 3: more state) and c (the spire
  stays on the ground: loses the open-air phase). **H5 = a** the boss attacks `lob` and `sweep`,
  built on the Polyp Mortar's lob and the Mantis's sweep with the spec's numbers; rejected: b
  (patterns the boss has: loses the telegraphed marker and laser) and c (lob only, `spiral` for
  the sweep). **H6 = a** the act-end order of Act 1 (kill, daylight, debrief with the act summary,
  outro with `act-complete` under page 1, hangar), so the draft's "the fanfare plays over the
  daylight section" is corrected; rejected: b (fanfare over the daylight, no jingle: differs from
  Act 1) and c (both: three fanfares in two minutes). **H7 = a** four outro pages, one per
  paragraph of the approved text, each with an image and voice (data in the Act 2 README);
  rejected: b (one page, one still) and c (images a/b). **H8 = a** round 36 auditions a/b for the
  root burst, the daylight outro, the tear-free roar and the canopy withering; the rest straight
  to production; rejected: b (also the Spire's death) and c (everything straight to production).
  **H9 = a** after the outro the hangar reads `BEFORE L15 - NOT BUILT YET`; M6 owns the end of the
  release; rejected: b (an end card, a new screen the design does not describe) and c (straight to
  the menu with the credits). **H10 = a** the Spire is built with the spec's HP, the fight is
  measured over several seeds and the HP tuned (not the attacks) to a mean of ≈ 135 s inside
  90–180 s, the par the measured mean; the bounty ≈ 30 % of the budget as paid (≈ 506, was 720 =
  43 %); rejected: b (keep 720 and let `bounty_scale` absorb it: breaks the 30 % share) and c (keep
  the HP and accept the measurement).
- 2026-10-11: M5 part H stated defaults (pending the user's review with the decisions): arena
  start, boss warning, arrival and checkpoint all at **t=108** (the draft's warning time; its
  boss arrival at t=113 and the checkpoint "at the warning" were two moments) and the Concord
  approach 95–108 (was 95–113); the clash at t=106/107/108 on top of the boss warning goes (the
  Choir t=98, Varga t=102, Rook's line is his boss-warning bark); sections as in *Layout* (the
  creep dropped, the draft's "16,550 px" and "≈ 17,800 px" were wrong with the creep); primary
  `reach-end` with the arena as the condition, "DESTROY THE SIEGE SPIRE" (the draft's `boss`
  primary is not one); secondary `groups` of the 8 growths, 63 in Act 1 terms, tracker `ROOTS
  n/8`; popcorn from t=8 and at least 50 enemies a minute (the draft's "no enemies before t=24"
  breaks the 3 s pacing rule; ≈ 92 units, was 42); the t=82 and t=104 rear Wraiths kept, the
  Hive Nodes releasing 2 Skitters once each in the budget; root bursts as in *Hazards*, the t=72
  Ravager crater a burst without growths; the data in Act 1 terms with a `bounty_scale`
  (estimate 0.6–0.8); the act HP factor for the returning medium units (Creeper 40, Hive Node,
  Ravager and Wraith at their L14 values), none for the Spire and the small units; the Spire's
  bounty ≈ 506 paid (pods 42, roots 42, maw 84, spire 170); the maw's Wraiths ambush plans
  paid at the normal Wraith bounty × 1.6 × the scale and budgeted at 4 launches (8); the
  budget(14) = 1,687 and the typical-haul table replace the draft's 2,410 "perfect collection";
  Varga's four intel lines (the draft had one) and two briefing pages; Rook's teaser attributed
  to him, his t=80 line dropped (his bark says it) and his t=3 line retimed after Okafor's, his
  phase-3 line an event line 5 s after the phase starts; the supply drone's patch a placed pickup
  without a drone sprite; the armoured car's crate 88 (pays 141, the draft's 140 was written in
  Act 2 terms); edge warnings 3 s; the daylight section without enemies or music, the canopy's
  tear a real-seconds timeline, the closing line queued `CLOSING`; "Firestorm" base from
  section 2 and full from 4, no mid-level jingle; the balance plan's L14 entry unchanged (`rear`
  L2, one Airstrike, Rook's Autocannon), re-checked by the measured fight; the threat profile
  computed from the data (the draft's "front 71 / rear 29" counted a different wave list); the
  secondary's wording "before they leave the screen" counted from the growth's placement; root
  burst hard timing: the first shot after 1.0 s, not the telegraph (the draft's wording read as
  if the telegraph shortened); L14's units in an atlas of their own with the Spire on its own
  pages. `design` goes to `review` for the reworked texts; `implementation` is `in-progress`.
- 2026-10-12: M5 part H, step H4 (pending the user's review with H1–H10): the level's `data.yaml` and
  the loader (`root_bursts`, the Siege Spire's boss keys, `PartHRules`/`PartHChecks`), the tables above
  rendered from it. Settled while the user is away: **the root field (section 3) scrolls at 80 px/s**
  (a growth appears 1 s after its dust and fires 1.5 s later: at 140 px/s it would by then have scrolled
  350 px from its burst and stood on the screen for a moment only; the bursts lie 100–140 px below the
  top edge, the least the simulation allows, so the growths fire about 200–240 px above the bottom edge
  and stand ≈ 3 s on the screen), the scroll totals 14,700 px; **the Spire's bounty 26.8 % of the budget
  as paid** (452; `bounty_scale` 0.76; at 30 % a perfect run stays under 1.4 × the budget); the
  arena section 108–250; the waves retimed to a wave every 3–3.5 s with smaller popcorn (95 wave units,
  112 with the ground units; 67.6 a minute), the root bursts' quiet window t=62–67, easy's t=82 Wraiths
  replaced by a Needler V-wing from the front (a Wraith flies only the rear ambush); the hard Molard
  nest of three Spine Turrets at x 200–280 between the nodes; the growths 52 px apart (the art's growth
  points), the Spine Turret left; the burst positions (t=63 x 150, 74 x 330, 84 x 170, 92 x 320) for
  the backdrop (step H5); the armoured car entering at t=14.5, x 330 (on screen ≈ 15–19 s), its secret's
  line given to Okafor; the supply drone's patch placed at t=100, x 240; the secondary named "Destroy
  every root-burst growth" with its tracker `ROOTS` (a `groups` objective may now name its tracker);
  the briefing's objective line `DESTROY THE SIEGE SPIRE` for an anchored act boss holding a
  `reach-end` level; Rook's phase-3 line a second `boss-phase` cue queued after Okafor's; the boss's
  launches budgeted at the measured three of 2 (`spawns`), not four; the intel strip's root-field mark
  the sections holding the bursts (60–95 s); the briefing images left out of the data until step H5
  renders them (as Level 13's); Rook's teaser shortened to "Roots first, flyers from behind, then the
  Spire. I'll take the six o'clock." and Varga's L2 and L3 lines shortened (the intel panel's room,
  `IntelPanelLayoutTest`).
- 2026-10-12: M5 part H, step H6 (the user is on vacation; **pending the user's review** in round 36):
  the game's looks and sounds. The root bursts draw look a (the cracks over the telegraph, the dust 6 px
  above the point over the telegraph and the eruption, the root's 0.5 s eruption leaving the crater) and
  grow their units in from 0.35 of their size, tinted from the creep's violet; the armoured car shows a
  frame a hit (one creep blob cleared per hit), its cleared frame for 0.2 s after the sixth, then the
  hatch open. The daylight runs on the Spire's death (real seconds): the canopy's veins fade out from 2
  to 7 s, the tear frames show from 0.5 to 8.5 s (2 s each, tiled on the high-air scroll), the beams
  ramp in from 2.5 to 5 s (the second frame from 6.25 s), as `backdrop_l14.py`'s review composite does.
  The ambience is the canopy's hum (`earth-canopy`), fading out over 2 s at the kill while the
  megacity's own ambience (with its distant sirens) rises over 7 s under the canopy's withering (0.5 s
  after the kill; 3 s since step H7b); round 36's sounds play as variant a. Rook's rear bark for the maw's launches is timed
  on the real clock (the level clock may halt in the arena). Pod 1's offset in the Spire's data had its
  sign flipped (−24.3, below the anchor, where its root's pod is 24 px above); fixed to 24.3 so the pod's
  hit box sits on its sprite (the measured medium fight is then 129.5 s, the par 130 s). The voices
  (20 lines: the radio, the two briefing pages, the Act 2 outro's four pages) are rendered; with their
  lengths the radio is retimed to the 1 s rule: Hammer Lead 16 → 13 (before Rook; his flight still
  passes during the line), Rook 7.5 → 21, Varga's canopy line 22 → 31.5, the Choir 98 → 96; three
  seeds are pinned (Hammer Lead's "Hammer flight", Varga's "hive nodes", Okafor's "ships out for Mars").
- 2026-10-12: M5 part H, step H7b (from the H7 capture; the user is on vacation, **all pending the
  user's review**): Rook's phase-3 line went stale behind Okafor's (an event line waits 6 s at most):
  a `boss-phase` cue now queues as a `PHASE` line, stale after 20 s, dropped at the kill. Okafor's
  root-grid line waited behind the Choir (96) and Varga (102) and played over the boss's arrival,
  which also made Rook's boss-warning bark (and his t≈104 rear bark, which the higher-priority warning
  bark replaces anyway) go stale: the secondary's line is dropped once a boss's warning starts. The
  growths' first-shot delay 1.5 → 0.75 s (hard 1.0 → 0.5 s): they fired about 220 px above the
  bottom edge, now about 280 px; the depths were already at the low end (100–140 px). The daylight
  drifts at 40 px/s instead of 140, so the stump stays on the screen while the light falls on it (the
  sim has no post-kill hold; a slower section is data only): 13,500 px of ground, the backdrop's strip
  sized by the daylight's speed (`backdrop_l14.py`, regenerated, `--check` 0 problems). Hammer flight
  passes at x 130 over the lake instead of x 340, where it hid the armoured car (x 330) at t 16–18.5.
  The canopy withering starts 3 s after the death (the death chain's end) instead of 0.5 s, where the
  chain's bursts masked it. The stump is drawn as muted ash (blended 60 % to a grey) so the free
  spire's dark star above it reads (darkening it made one dark mass). Kept: the death's white flash
  (the act bosses' death flash, the same as the Brood Carrier's in Level 07, design/enemies/bosses)
  and the silent act summary page (Act 1's is the same: the debrief plays no music, the outro's
  `act-complete` follows).
- 2026-10-12: M5 part H, step H9 (from the H8 capture `level-14-capture-fixes-r36-a`; the user is on
  vacation, **all pending the user's review**): (1) Okafor's root-grid line never played: the roots
  die at about t=93.5 with the radio free, but at the default text speed the line holds the radio
  about 10 s (two pages), so it found no gap before the Choir (96) or Varga (102) and was dropped at
  the Spire's warning. A closing line queued while the radio is free (nothing on it, nothing waiting)
  now goes ahead at once and the timed lines that fall due wait for it (`RadioQueue`, design/ui/hud):
  it plays at 93.5, the Choir about 104, Varga about 110 (over the Spire's arrival). Rejected: moving
  the Choir to 98–99 (the line ends about 104: the three lines take about 23 s of the 15 s before the
  warning, so no retiming keeps the 1 s rule). Roots that die late, while a line plays, still wait for
  a gap and are dropped at the warning (`RadioTimelineTest`). (2) The daylight drifts at 25 px/s
  instead of 40: the stump left the bottom edge about 9 s after the kill, 10 s before the debrief; now
  about 21 s after it (measured in `level-14-capture-daylight-r36-a`: the body leaves at about 212, the root tips at about 215; the debrief at about 217.5), the scroll
  running on to the debrief; 13,320 px of ground; the backdrop regenerated
  (`backdrop_l14.py`, `--check` 0 problems), its proposal merged (the strip's top piece and the
  daylight boulevard later). (3) The t=100 armour patch is carried in by Level 13's CDF supply drone,
  flying in from below the bottom edge (no helipad here), releasing it at x 240, 380 px up and
  leaving the same way (the data's `drops`); without a relay no launch sound plays.
