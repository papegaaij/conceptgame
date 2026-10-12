---
title: Level 13 – Polar Relay
design: review
implementation: in-progress
art: proposed
depends-on: [../../../enemies/naval/skimmer, ../../../enemies/ground/scuttler, ../../../enemies/air/stinger, ../../../enemies/ground/polyp-mortar, ../../../allies, ../../../systems/economy]
updated: 2026-10-12
---

# Level 13 – Polar Relay

## Summary

The first `defend` mission. **Nansen Relay**, on the Arctic pack ice, ties every gun, radar and
satellite on Earth into one defence grid, and a Vrell landing force is moving on it. After a
short approach the scroll halts over the relay, and Lancer and Rook hold it for three minutes
while it synchronises the grid. Waves come from every edge. The level introduces the
[Skimmer](../../../enemies/naval/skimmer/README.md), which weaves through the leads between the
floes, and brings back the [Scuttler](../../../enemies/ground/scuttler/README.md) striding across
the ice. A whiteout rolls in for the final push. About 3 minutes 55 seconds; no boss.

**Status (M5 part G, step G1a).** This document states the design decided on 2026-10-11 (the
gap analysis's questions G1–G9, all taken as the recommendation **a** while the user is on
vacation, and its stated defaults; **all pending the user's review** in the combined concept round
at the end of M5, see *Decisions*) and is the ticket for the later steps. Its script is
[data.yaml](data.yaml) (step G4): the marked tables below are rendered from it. The level reworks the
approved draft for the M4/M5 rules: density, the 3 s pacing rule, budget(13) in Act 1 terms,
voiced radio, edge warnings of 3 s and the exact hook modes.

## Briefing

Two pages, as Levels 08–11, each **one screen** (`BriefingLayoutTest`): Okafor's mission (the
relay and the landing), then Varga's warning about the boats and the walkers. Draft texts, for
review in concept round 35:

<!-- data: briefing -->
> **Commander Okafor:** "Lancer, Aegis Actual. Nansen Relay ties every gun on the planet into
> one defence grid, and a Vrell landing force is crossing the pack ice to it. Hold over the
> relay for three minutes while it synchronises. They will come from every side. Aegis Actual
> out."
>
> **Dr. Varga:** "Fast boats weave in the leads: lead your shots or spray. Walkers' claws are
> armoured in front, so hit them from the side. Shots at the relay are aimed at it: stand in the
> way. Side and rear guns will earn their keep."
<!-- /data -->

*Images* (production art straight away, as Level 11; [briefing images](../../../ui/briefing/README.md)):
Okafor's page `level-13-relay-grid` (the planet's grid with Nansen Relay marked, the landing
force's approach over the pack ice) and Varga's `level-13-ice-field` (the floes and leads from
above: a Skimmer's lane, a Scuttler's path, the relay in the middle), rendered in step G5 by
`tools/art/briefing_images.py` (round 35's sheet [briefing-images-final-r35-a](../../../ui/briefing/concept/briefing-images-final-r35-a.png))
(not yet the pages' `image` in the data: with an image a page holds four lines on one screen and the
texts run to eight and five, `BriefingLayoutTest`; shorter texts are proposed for round 35). Okafor's shows the Arctic from above the pole with the
grid the relay ties together and an inset of the halt's frame (the final backdrop) with the landing
force closing from every edge; Varga's is the halt's frame as a scan, a Skimmer weaving along the south
lead, a Scuttler's armoured front arc and the side shots, a shot aimed at the relay with the ship in
its way.

*Hangar teaser* (shop screen before L13; Rook, `LT. K. TANAKA / AEGIS TWO`, not voiced, as Level 10's):

<!-- data: teaser -->
> **Rook:** "Defend the Arctic relay for three minutes. Attacks from every edge: rear and side
> guns pay off."
<!-- /data -->

*Varga's intel line* per sensor-suite level (the data's `varga`; four lines, the draft had one;
fitting the intel panel is checked by `IntelPanelLayoutTest`):

| Sensor suite | Line |
|---|---|
| none | "Pack ice, Lancer. Boats in the leads, walkers on the floes, and no front line out there." |
| L1 | "They'll come from all four edges. Side and rear guns will earn their keep." |
| L2 | "New: the Skimmer, a fast boat that weaves. The walkers are back, and the divers." |
| L3 | "Spread fire for the boats, side shots for the walkers. Hold the relay for three minutes." |

## Threat profile

The directions are counted by each wave's `from` (`cross` waves split evenly over the four edges).

<!-- data: threat-profile -->
| Field | Value |
|---|---|
| Dominant layers | `ground` (Scuttlers on the floes, the Polyp Mortars, the Skimmers on the leads, naval surface), `air` |
| Attack directions | front 46% · sides 29% · rear 25% (a `defend` level: **excepted** from the campaign's "at least ~60 % of waves enter from the top edge" rule; the `cross` waves split evenly over the four edges) |
| Density | 4 |
| Recommended traits | `spread`, `rear`; `side` helps too (three traits do not fit the intel panel) |
| Hazards | Whiteout (edge visibility, final push); the relay must survive |
| Boss / mid-boss | none (finale: the whiteout push) |
| Sensor-suite detail | **none**: arctic, ground + air, all directions. **L1**: + the directions, density 4, hazard "whiteout", the objective `DEFEND NANSEN RELAY`. **L2**: + Skimmer (new), Needler, Scuttler, Mantis, Stinger, Skitter, Polyp Mortar portraits. **L3**: + `spread` and `rear` highlighted; the timeline strip marks the three defend phases and the whiteout (new marks, step G6); 1 secret. |
<!-- /data -->

## Objective

- **Primary** `defend` (the first one; a new primary kind, see
  [schemas](../../../tech/architecture/README.md#data-file-schemas)): Nansen Relay must survive the
  180 s synchronisation (t=40–220). It builds on the escort's machinery (one ally with
  integrity, relay-aimed hits, the failed-primary flow) and pays no primary credits; there is no
  checkpoint (no boss).
  - **Fail**: relay integrity reaches 0 → mission failed (see [retry](../../../systems/retry/README.md);
    the mission-failed screen as for a lost escort; the lost-mission prompt's fly-on option stays
    unbuilt). The radio plays Okafor's "Nansen is gone".
- **Secondary** "Integrity ≥ 50 %" (new kind `integrity`): the relay at or above 50 % of its
  integrity when the synchronisation completes (t=218). **Failed at once** when it drops below the
  share (there is no regeneration, so it cannot recover), **met** at t=218. Pays **95** credits in
  Act 1 terms (152 in Act 2, the credit factor 1.6); the typical haul counts the economy's secondary
  share (50 %).

### Nansen Relay

The [Nansen Relay](../../../allies/README.md#nansen-relay) (footprint 96×96, hit box 80×80, integrity
240 at medium as tuned below, damage rules in the allies spec) stands on the `ground` layer at play-field
**x = 240** (the centre; the draft's "x = 480 in screen coordinates" was the 960-wide screen), y = 290
from the bottom (y-up). It is `follows: ground`: it scrolls in at the top edge during the approach
(**entering at t ≈ 37 at 140 px/s**; the draft's "appearing at the top at t≈34" was not possible) and
stops where it is because the ground halts (see *The halt*). Player fire, Rook's fire and the specials
pass over it, and it has no contact with the ship. It is not repaired and has no primary credits; the
crew launch the supply drones for the player (see *Secrets and pickups*).

Feedback: a white hit flash per hit; **smoke below 50 %**; at 0 it goes dark with a smoke column and
the mission fails; at grid online (t=220) its lights come on in sequence.

**Integrity per difficulty** (**G2 = a**: tuned, not guessed): the starting values are **600 medium /
900 easy / 450 hard** from the 82-unit draft. The level adds unhooked popcorn, so the relay's load is
the hooked waves and how fast they die. The content step measures the relay's outcome with the defend
autopilot over several seeds (as Level 11's cargo ships) and tunes the integrity until **medium never
loses the relay and keeps ≥ 50 % in about half the seeds**; **easy keeps it nearly always**; **hard may
lose the secondary** (hard is supposed to be hard) but **never the relay**. The measured integrity at
the end per difficulty is recorded here (and in round 35).

**Measured (step G4, pending the user's review).** The guarding autopilot (`Autopilot`: it lines up on
a mortar lobbing at the relay first, then a diver sent at it, then a walker with the relay in its arc,
then the nearest hooked boat, and holds station under the relay otherwise) with the plan's fit and Rook,
eight seeds per difficulty (`BalanceTest.theAutopilotKeepsTheRelay`; damage taken by the relay up to the
synchronisation's end):

| Difficulty | Damage taken (8 seeds) | Integrity | Left at t=218 | Secondary met |
|---|---|---|---|---|
| Easy | 48–56 | **340** (draft 900) | 84–86 % | 8 of 8 |
| Medium | 112–124 | **240** (draft 600) | 48–53 % (median 50 %) | 7 of 8 |
| Hard | 386–410 | **520** (draft 450) | 21–26 % | 0 of 8 |

Nearly all of it is the Scuttlers' fans in arc (switching the hook off kind by kind, measured before
the popcorn's last retiming: on medium the Scuttlers ≈ all of it, the Skimmers ≈ 4, the mortar floe and
the Stingers 0, as the autopilot kills the floe and the divers before they reach the relay; on hard the
Scuttlers ≈ 90 %, the mortars ≈ 50, the Skimmers ≈ 36). With the draft's 600 the autopilot kept 81–83 %
on medium in every seed, so G2's rule lowered medium to 240; hard's load is 3.4 × medium's (7-way fans,
the formation lever's larger pincers, tougher walkers), so hard's relay needs **more** integrity than
medium's to never fall: the bar is relative (the HUD shows a share), and hard stays the harder level (the
relay ends at 21–26 %, the secondary never met). The damage comes in bullets of 4 and hardly varies by
seed (the waves are formations, not scatter), so "about half" is the median seed ending at exactly 50 %:
a coin flip at the threshold for a player who guards as well as the autopilot (228 met 1 of
the 8 seeds, 240 meets 7). **For the user's review**: the three values; a human player who lets the
walkers fire longer than the autopilot will lose the relay on medium with 240 (the draft's 600 would
forgive about five times the autopilot's load).

#### What hurts the relay: the hook modes (G1 = a)

Relay targeting uses the
[target-the-objective hook](../../../enemies/README.md#target-the-objective-hook), with a mode **per
enemy kind** and a difficulty override (`targeted_by: {skimmer: alternate, scuttler: in-arc, stinger:
always, polyp-mortar: always}`; easy: `stinger` none, so on easy the Stingers dive at the player). The
popcorn (Skitter, Needler) and the Mantis are never hooked, so the relay's load stays with the
authored hooked waves. All modes are on the **relay-aimed rule**: only an attack aimed at the relay
hurts it; the bullets still hurt the ship, so the player can body-block (as the crawler rule).

| Units | Mode | Exact rule |
|---|---|---|
| Skimmers (all waves) | `alternate` | Each unit counts its attacks; **every second attack** goes at the relay **while the relay is closer than the ship**, the others at the ship. A hard 2-shot burst is **one** attack |
| Scuttlers (all waves) | `in-arc` | While the relay lies within **±45° of its facing** (its armour's arc), its fan is **centred on the relay** and relay-aimed; otherwise the fan fires as before. The acid spit stays on the ship |
| Stingers (t=156 and t=210 waves) | `always` | The dive is aimed at the relay's **centre**, and its fan is centred on the relay. **Crossing the relay's footprint deals the spec's 10 once**; the Stinger then flies on off the bottom edge (it stays shootable and does **not** die on the relay: it is a diver, not a kamikaze) |
| Polyp Mortar floe (t≈66) | `always` | Each lob is aimed at the relay: the marker lies on it, the direct hit (`heavy`) lands on the relay and the ring hurts only the ship |

## Layout

Approach at **140 px/s**, then the scroll **halts** for the defend phase, then resumes for the
exit. Total ≈ 235 s: 40 s approach, 180 s halted, 15 s exit. While halted, the loose floes and brash
ice keep drifting at 6 px/s inside the open water's margins (they keep out of the lanes) and the
open water in the leads moves gently. Motion budget: the drifting floes with their waterline foam
and, in phase 3, the whiteout banks are the strong elements; the fog wisps stay faint.

### The halt

A **timed halt** (new: until now a halted section was only a boss's arena): a section with `speed: 0`
and no `arena`, allowed only with a `defend` primary.

- **The clock runs on** (no slowing, no boss): the waves at t=40–220 keep their authored times.
- **Eases**: the scroll slows from 140 to 0 over the last **2 s** of section 1 (t=38–40) and
  back up over **2 s** from t=220 (a section `ramp`, defaults); the relay is placed so that it stops at
  (240, 290 y-up) with the eased scroll (as Level 11's `haltScroll`).
- **Nothing scrolls in** during the halt: a ground unit, ground object or placed pickup timed inside
  t=40–220 that enters "at the top edge with the scroll" would sit at the edge for minutes, so the
  validator rejects it. The halt uses walkers on lanes, side drifts and point drops.
- Everything keyed to the scroll would freeze, so it runs on the **real clock** in the halt: the snow
  on `high-air`, the fog wisps, the loose ice, the wakes.

<!-- data: level-sections -->
| Section | t (s) | Scroll (px) | Speed (px/s) | Atmosphere | Layers and content | Purpose |
|---|---|---|---|---|---|---|
| 1. Pack Ice | 0–40 | 0–5,460 | 140 | light | `deep`: dark open water under the ice. `ground`: white-blue ice floes with open leads, teal submerged ice edges, waterline foam (Water rules), an abandoned research hut (the secret), the relay entering at the top at t≈37. `low-air`: thin fog wisps. Low sun, long cool shadows. | Skimmer introduction during the approach; the scroll eases to 0 over t=38–40, arriving at the relay. |
| 2. First Light | 40–100 | 5,460–5,460 | 0 | light | `ground`: Nansen Relay at the centre on its floe, the lanes (leads) cut round it, loose floes drifting slowly, Skimmer wakes in the leads. | Learn to guard a point: boats from all edges. |
| 3. Floe Walkers | 100–160 | 5,460–5,460 | 0 | light, medium peak 140–160 | `ground`: as above, with Scuttlers striding across the floes from the sides. `low-air`: fog thickening at the edges from t=140. | Walkers with armoured fronts: hit them from the side; side attackers. |
| 4. Whiteout | 160–220 | 5,460–5,460 | 0 | medium, heavy peak 176–188 | `low-air`: whiteout banks rolling in from the side edges, 25–40 % coverage at the edges, the centre around the relay kept readable. `high-air`: snow streaks (≤ 40 %) falling on the real clock. | The final push in poor visibility; edge warnings still show through the whiteout. |
| 5. Grid Online | 220–235 | 5,460–7,420 | 140 | clear | `ground`: the relay's lights coming on in sequence, then floes and open sea northward. | Release; level end. |
<!-- /data -->

The scroll column is the data's, with the 2 s eases (the approach ≈ 5,460 px to the halt, the exit
≈ 1,960 px; the draft's 5,600 / 7,700 px were without them).

**The lanes and the leads (G4 = a).** A Skimmer is a **lane walker**: each follows a *lane*, a
ground-space polyline given by its wave (as a walker's path), at 180 px/s with the 40 px / 1.2 s weave
as an offset, facing its velocity (16 headings). The lanes scroll with the ground in the approach and
stand still in the halt. The backdrop generator reads the lanes and **cuts the leads along them**; the
relay's floe and the leads are fixed in the halt, and the 6 px/s drift is loose floes and brash ice
inside the open water's margins, keeping out of the lanes (the leads do not drift: over 180 s that
would be 1,080 px).

**Water and ice (G5 = a).** `water: true` covers the whole play field (as Levels 11 and 12, E1), so a
torpedo also reaches a Scuttler on a floe. The presentation goes **by unit, not by place** (a level
flag on E's rule "every bomb, shell and Airstrike burst on a water level is the water splash"): in this
level only a burst on a naval unit (a Skimmer) and the Skimmer's death use the water splash; every
other burst (on Scuttlers, mortars, the hut, or on nothing) is the ground burst. No `sub` units in this
level.

**Whiteout (G7 = a).** The banks are on `low-air`, so by the layer model they cover only `ground`
units: the boats and walkers come out of the white at the side edges, while air units, bullets, Rook
and the warnings draw above them. Snow streaks on `high-air`; the centre round the relay is kept
clear; edge warnings for every side wave. Draw order and art only: no veil over the air units.

## Waves

Enemy definitions are linked per row; formation names come from the
[formation vocabulary](../../../enemies/README.md#formation-vocabulary). Data in **Act 1 terms** with a
`bounty_scale` (see *Credit budget*); returning units use the act HP factor from the
[balancing basis](../../../enemies/README.md#balancing-basis) (Scuttler, Mantis; none for the Skimmer,
its first level); HP values live in the enemy specs only. Every side and rear wave gets the **3 s**
edge warning (the draft's "1.5 s" was superseded on 2026-10-02), the whiteout's too.

**Density (G3 = a).** The level counts from the launch to the end of the clock (≈ 231 s: the halt is
not an arena, so halted time counts as scroll time), so **≥ 50 units a minute (Act 2's minimum)**
needs ≈ 196 units instead of the draft's 82. The extra **114** are **unhooked popcorn** the act knows
(Skitter streams, Needler pairs and lines) from all edges, so the relay's load stays with the authored
hooked waves; the t=20 introduction stays alone and the 3 s pacing rule holds (`PacingTest`: no empty
screen over 3 s). Rule changes: none (a `defend` level's density is the act minimum).

Directions are by `from`; `cross` waves (new formation) send groups from **all four edges**, n per
edge, each unit on its own lane, entering together with an edge warning on every edge.

The waves as the data has them: the **hooked and featured waves** of the approved draft (retimed only
where marked) and the **popcorn** (unhooked: Skitter streams and line abreasts, Needler lines and
pincers, from all edges). Changes against the draft's plan (step G4): Skitters reach the rear as a line
abreast, or the t=140 rear stream as a snake on a path (only chasers stream in from the rear); the t=200
rear Needlers fly a line abreast (a V-wing enters only from the front). The Skimmers' lanes in the halt
run along the leads round the relay's floe: the north lead (y ≈ 95 px below the top edge), the south lead
(y ≈ 460), the west lead (x ≈ 75) and the east lead (x ≈ 405), the relay at (240, 250); each cross unit
on its own lane, in from its edge, round a corner of the floe and off another edge. The Scuttlers' paths
walk toward the relay, stop ≈ 150 px short and turn along it (the relay in arc for a fan or two).

<!-- data: waves -->
| t (s) | Section | Formation | Enemies (link) | Count | Enter from | Notes |
|---|---|---|---|---|---|---|
| 5 | 1 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (alternating edges) | The first targets over the pack ice |
| 11 | 1 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 3 | front | |
| 20 | 1 | convoy | [Skimmer](../../../enemies/naval/skimmer/README.md) | 3 | front | **New.** Weaves along a lead between the floes; nothing else on screen |
| 24 | 1 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | After the introduction |
| 30 | 1 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 5 | front | |
| 33 | 1 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | |
| 37 | 1 | pincer | [Needler](../../../enemies/air/needler/README.md) | 2 | sides | |
| 40 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | sides (alternating edges) | |
| 48 | 2 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 3 | rear | |
| 51.5 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | |
| 56 | 2 | cross | [Skimmer](../../../enemies/naval/skimmer/README.md) | 4 | all edges | First all-edge wave, one from each edge; Rook: "pick a direction" |
| 58.5 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | |
| 68 | 2 | pincer | [Needler](../../../enemies/air/needler/README.md) | 4 | sides | 2 per side |
| 70.5 | 2 | line abreast | [Skitter](../../../enemies/air/skitter/README.md) | 6 | rear | |
| 74 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | |
| 81.5 | 2 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 3 | front | |
| 84 | 2 | convoy | [Skimmer](../../../enemies/naval/skimmer/README.md) | 4 | rear | Along the lead below the relay |
| 88 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | left side | |
| 93 | 2 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 3 | front | |
| 95.5 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | right side | |
| 100.5 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 3 | front (alternating edges) | |
| 104 | 3 | pincer | [Scuttler](../../../enemies/ground/scuttler/README.md) | 4 | sides | Walk across the floes toward the relay and turn along it, 2 + 2 (the relay in arc for a fan or two); claws armoured from the front |
| 108 | 3 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 3 | rear | |
| 112 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | |
| 118 | 3 | cross | [Skimmer](../../../enemies/naval/skimmer/README.md) | 4 | all edges | Hard: 2 per edge (8) |
| 121 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | sides (alternating edges) | |
| 124 | 3 | pincer | [Mantis](../../../enemies/air/mantis/README.md) | 2 | sides | Hold at the edges and sweep; the first one drops an overdrive |
| 128 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | |
| 132 | 3 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 3 | front | |
| 135 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | |
| 140 | 3 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 12 | rear | Up from the rear on one path |
| 146 | 3 | convoy | [Skimmer](../../../enemies/naval/skimmer/README.md) | 4 | left side | |
| 150 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | |
| 156 | 3 | V-wing | [Stinger](../../../enemies/air/stinger/README.md) | 4 | front | They dive at the **relay**, not the player (`always`; easy: at the player) |
| 158.5 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | sides (alternating edges) | |
| 164 | 4 | pincer | [Scuttler](../../../enemies/ground/scuttler/README.md) | 4 | sides | |
| 168 | 4 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 3 | rear | |
| 172 | 4 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | |
| 176 | 4 | cross | [Skimmer](../../../enemies/naval/skimmer/README.md) | 8 | all edges | Into the whiteout, 2 from each edge |
| 180 | 4 | line abreast | [Skitter](../../../enemies/air/skitter/README.md) | 6 | rear | |
| 184 | 4 | pincer | [Mantis](../../../enemies/air/mantis/README.md) | 2 | sides | |
| 188 | 4 | circle | [Needler](../../../enemies/air/needler/README.md) | 6 | front | Orbits **above the relay** (a circle centre `at`, 150 px from the top), then breaks toward the player |
| 192 | 4 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | sides (alternating edges) | |
| 196 | 4 | convoy | [Scuttler](../../../enemies/ground/scuttler/README.md) | 3 | front | Walks down the main floe |
| 200 | 4 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 3 | rear | |
| 204 | 4 | cross | [Skimmer](../../../enemies/naval/skimmer/README.md) | 4 | all edges | |
| 206 | 4 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | |
| 210 | 4 | column | [Stinger](../../../enemies/air/stinger/README.md) | 4 | front | The last push: diving at the relay one after another; hard: in pairs (0.4 s between pairs) |
| 213 | 4 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | sides (alternating edges) | |
| 216 | 4 | line abreast | [Skitter](../../../enemies/air/skitter/README.md) | 6 | rear | |

Totals: Skitter 111 · Needler 41 · Skimmer 31 · Scuttler 11 · Mantis 4 · Stinger 8.
<!-- /data -->

Medium has 208 units (≈ 54 a minute over the 230 s from the launch: `DensityTest`), easy 160, hard 260:
12 more than the draft's plan (196), as the pacing rule needs the popcorn as streams of 4 at 2–2.5 s
intervals every 3–5 s rather than streams of 6 at 1 s (the plan fit's guns clear a short stream in
seconds), with a stream at t=24 after the introduction and the intro's Skimmers three on easy too. The
pacing rule (nothing empty over 3 s, `PacingTest`) holds on every difficulty but for its two allowed
pauses: the calm before the t=20 introduction (≈ 13–20 s) and the exit after t=219 (the release).

## Ground targets

<!-- data: ground-targets -->
| Section | Target | Effect |
|---|---|---|
| 1 | Abandoned research hut, its roof hatch blinking (trigger, t≈12) | 3 hits on the hatch release the hidden crate (the secret) |
| 2 | [Polyp Mortar](../../../enemies/ground/polyp-mortar/README.md) ×2 on **one floe** entering from the left edge and drifting right at 10 px/s (t≈66) | A nest entering at a side edge with a drift. They target the relay (`always`): each lob's marker lies on it, the direct hit lands on it; ≈ 13 lobs each while the floe crosses unless killed (the gap analysis's biggest relay risk; measured in step G4, the guarding autopilot kills the floe before it lands a lob on medium, ≈ 50 damage on hard: the Scuttlers' fans are the load); side-edge warning; credits |
<!-- /data -->

## Hazards

- **Whiteout** (176–188 peak): banks on `low-air` cover the side edges of the play field and hide the
  ground units there (G7); enemy bullets, air units and edge-warning arrows always draw above them.
- Floes are scenery on the `ground` layer: no collision.

## Secrets and pickups

- **Research hut cache** (t≈12): three hits on the blinking roof hatch release a CDF cache worth
  **110** credits in Act 1 terms (176 in Act 2; the draft's 180 was an act-scaled amount).
- **Supply drones** (t=160 and t=200; **G6 = a**): the relay crew launch a drone carrying an armour
  patch. The drone is **presentation only**: it lifts off the relay's helipad, flies a slow arc
  (≈ 4 s) over the relay and **releases the patch at the arc's top**, a point in the field
  (≈ (240, 380 y-up), at t≈162 and t≈202); the patch is then an ordinary pickup (it drifts down, the
  magnet pulls it, it expires as pickups do). Nobody can hit the drone. Rook does not collect
  pickups. In the simulation: a pickup released at (t, x, y).
- **Overdrive**: dropped by the first Mantis of the t=124 pincer.

## Radio chatter

Spoken radio ([voice](../../../audio/voice/README.md)), as Levels 08–11. The draft's script is
**retimed to the HUD's 1 s rule** (`RadioTimelineTest`): a timed line holds the radio for the longer
of its voice and its text, then 0.4 s of silence. The draft clashed at t=1/3, 36/40, 172/174 and
218/220 (and the secondary's line); step G4 retimed it with every line as text (Nansen is uncast,
the voices come in step G6, which fixes the times again against them): Okafor t=1, Varga t=13 (before
the introduction at t=20), Rook t=24; Nansen t=36 and Okafor t=47.5, Rook t=58.5 (during the first
cross); Varga t=102 and Okafor t=154; Nansen's drone line at t=162 (the release); Okafor t=172 and the
Choir at t=179; Nansen t=218, the secondary's line queued behind it and Okafor's "Well held" after both
at t=233.5; Rook's "palm trees" quip is the `level-end` line (no room for a timed line after Okafor's).
The secret's line is Varga's ("A CDF research cache. Somebody left that hut in a hurry.", new in step
G4). Rook's t=82 "Six o'clock" and
t=122 "Contacts on your flank" are **dropped** (his rear and side barks say them); his timed lines
`requires: escort`. The integrity lines are **events**: `ally-below` with the share as subject (50 %
Nansen, 25 % Okafor; once each), `secondary-objective` and `mission-failed`.

**New speaker**: the **Nansen** CDF officer, a new voice auditioned a/b in round 35 as Atlas Control was
(`tools/concept/audio/tts_r35.py`); `uncast` until the round picks. ≈ 20 lines, ≈ 6 of them Nansen's,
Varga's four intel lines, two briefing pages and Rook's teaser (not voiced).

<!-- data: radio -->
| Trigger | Speaker | Line |
|---|---|---|
| t=1 | Okafor | "Nansen Relay coordinates every gun on the planet. If it goes dark, so does the grid." |
| t=13 | Varga | "Fast boats in the leads. They weave. Lead your shots, or spray." |
| t=24 | Rook | "Cold enough for you, Lancer? My canopy's frosting on the inside." |
| t=36 | Generic CDF (Nansen) | "Aegis, Nansen. Very glad to see you. They come at us from every direction out here." |
| t=47.5 | Okafor | "Lancer, you're holding station over the relay. Nothing touches it." |
| t=58.5 | Rook | "Contacts, all sides! Pick a direction, any direction." |
| t=102 | Varga | "Walkers on the floes. Their claws are armoured from the front. Hit them from the side." |
| t=154 | Okafor | "Divers inbound. They're going for the relay, not you." |
| t=162 (the first drone's release) | Generic CDF (Nansen) | "Supply drone away. Catch it, Aegis." |
| t=172 | Okafor | "Weather's turning. Whiteout coming in from the edges." |
| t=179 | The Choir (distorted) | "[the Choir sings]" |
| Relay below 50 % (`ally-below` 0.5) | Generic CDF (Nansen) | "Integrity at half! We can't take much more!" |
| Relay below 25 % (`ally-below` 0.25) | Okafor | "Lancer, the relay is failing. Everything else can wait." |
| t=218 | Generic CDF (Nansen) | "Grid sync complete. Every battery on Earth just came back online." |
| Secondary objective met | Generic CDF (Nansen) | "Integrity holding above half. You kept the lights on, Aegis." |
| t=233.5 (t=233.5 (after the secondary's line)) | Okafor | "Well held, Aegis. Now we can see the whole board." |
| Level end | Rook | "Next time they need a relay defended, I'm asking for one with palm trees." |
| Relay destroyed (`mission-failed`) | Okafor | "Nansen is gone. Pull back, Lancer." |
<!-- /data -->

## Boss / mid-boss

None. The whiteout push (t=176–220) is the finale.

## Music & ambience

The Act 2 A theme "Homefront" (track 6 in the [track list](../../../audio/music/README.md#track-list),
final with its base stem): the [arctic ambience](../../../audio/sfx/README.md#ambience-per-setting)
(`ambience-arctic-r08-a.ogg`, cold wind; a new key `AMBIENCE_ARCTIC`) alone in section 1, the **base
stem from section 2**, the **full mix from section 4** (the whiteout); the ambience stays under the
music, about +3 dB in the whiteout peak. **The mission-complete jingle (G8 = a)**: at t=218 "Homefront"
fades out over 2.6 s while grid online's chord rings, and the jingle (track 23) plays **once** as the
chord ends (t=220.6) under Nansen's "Grid sync complete", then the
ambience alone to the end; **the debrief starts without its jingle** for this level (a level key).
New sounds (relay hit, integrity alarm at 50 % and 25 %, relay destroyed as a power-down, drone launch,
grid online as a power-up chain; the Skimmer's flip-and-sink reuses the water explosion) are listed in
[sfx](../../../audio/sfx/README.md).

## Credit budget

Budget(13) = 700 × 1.07¹² ≈ **1,577**, the typical haul's target
([economy](../../../systems/economy/README.md#per-level-budget)), not a perfect collection (the
draft's 2,252 was the old perfect-run curve). The level's **`bounty_scale`** puts the typical haul on
it: **0.67** (the plan: 0.6–0.7 at ≈ 190 units), the typical haul 1,606 (+1.8 %, `TypicalHaulTest`:
±5 %); the popcorn is cheap (Skitter, Needler) so the scale stays at its lower end.
**The data keeps Act 1 terms** (default): the stat-block bounties (Skitter 5, Needler 12, Stinger 15,
Mantis 30, Skimmer 14, Scuttler 25, Polyp Mortar 15), the hut's cache **110** and the secondary
**95**; the code pays each × the act factor 1.6 (bounties also × the scale), rounded per kill. The
typical haul counts air kills at 60 %, ground kills (Skimmers, Scuttlers, mortars) at 80 % and the
secret and the secondary at 50 %.

<!-- data: credit-budget -->
| Source | Perfect run | Typical haul |
|---|---|---|
| Kills: Skitter 111 × 5 + Needler 41 × 12 + Skimmer 31 × 14 + Scuttler 11 × 25 + Mantis 4 × 30 + Stinger 8 × 15 | 2,106 | 1,416 |
| Ground targets: Polyp Mortar 2 × 15 | 32 | 26 |
| Secret: research hut cache (hidden crate, 7% of budget) | 176 | 88 |
| Secondary: keep the relay at 50 % or more | 152 | 76 |
| **Total** (bounty scale 0.67) | **2,466** | **1,606** |
| Budget(n) = the typical haul's target; typical +2 %, perfect 1.56 × budget | | 1,577 |
<!-- /data -->

**Balance plan** (default): the plan's L13 visit is as written (Armour Mk III, both Micro-missile Pods
to level 2); Rook keeps the Autocannon fitted at L12 (the Skimmers and Scuttlers are hit by standard
shots; the Mortar's lob is slow against 180 px/s boats). `BalanceTest` gets the Skimmer's time to
kill and a relay-outcome check on the autopilot. Rook does not guard the relay (no change to his AI).

## Difficulty notes

- **Easy**: relay integrity 340 (tuned, see *Nansen Relay*); the introduction's three Skimmers (an authored count); the whiteout peak lasts 6 s (presentation,
  step G6: the data's peak is medium's 176–188); the Stingers dive at the player
  instead of the relay (`stinger` unhooked); Skimmer HP and bursts per their spec.
- **Hard**: relay integrity 520 (tuned; hard's hook load is 3.4 × medium's); the t=118 cross has 2 Skimmers per edge; the t=210 Stingers
  dive in pairs; the Skimmers fire 2-shot bursts (one attack under `alternate`); the Scuttlers' fan
  is 7-way. Hard may lose the secondary, never the relay (G2).

## Concept art

The look comes from the chosen arctic scene in
[art direction](../../../art-direction/README.md):
[scene-arctic-r09-a.png](../../../art-direction/concept/scene-arctic-r09-a.png) and its
[scroll loop](../../../art-direction/concept/scene-arctic-r09-a.gif), which already shows the
relay and Skimmers (generator `tools/concept/scenes_r09.py`). **Round 35 (G9 = a)**: production
straight away for the arctic backdrop (`tools/art/backdrop_l13.py`) and the Skimmer (chosen concept
[skimmer-r06-a](../../../enemies/naval/concept/skimmer-r06-a.png)); **a/b** for **the relay** (its
states and the lights sequence) and **the whiteout peak** (a still each); **one version** for the
drone and the research hut; **sounds a/b** for the relay hit, the integrity alarm and grid online (one
version for the drone launch); **Nansen** auditioned a/b as Atlas Control was.

Round 35's level files (step G2; the relay's a/b is in [allies](../../../allies/README.md#concept-art),
the Skimmer's in its own README, the sounds and Nansen in [sfx](../../../audio/sfx/README.md) and
[voice](../../../audio/voice/README.md)); generator notes in [concept/prompts.md](concept/prompts.md):

| File | What | Status |
|---|---|---|
| [concept/whiteout-r35-a.png](concept/whiteout-r35-a.png), [.gif](concept/whiteout-r35-a.gif) | Whiteout peak a "rolling banks" (`tools/art/whiteout_l13.py`): the halted frame at the peak, the layers, the ramp; a 4 s loop | proposed |
| [concept/whiteout-r35-b.png](concept/whiteout-r35-b.png), [.gif](concept/whiteout-r35-b.gif) | Whiteout peak b "blizzard curtain": as a | proposed |
| [concept/l13-props-final-r35-a.png](concept/l13-props-final-r35-a.png), [.gif](concept/l13-props-final-r35-a.gif) | Production art, one version each (`tools/art/l13_props.py`): the CDF supply drone (rotor phases, clamps, nav lights, the patch in its bay; its arc from the helipad) and the research hut (four states, the hatch beacon) | proposed |
| [concept/backdrop-final-r35-a.png](concept/backdrop-final-r35-a.png), [.gif](concept/backdrop-final-r35-a.gif) | Production backdrop straight from scene-arctic-r09-a (`tools/art/backdrop_l13.py`, notes in [concept/prompts.md](concept/prompts.md#backdrop-final-r35-a); its block for the level data, merged, is [concept/backdrop-proposal.yaml](concept/backdrop-proposal.yaml)): the ground as one unique strip of four pieces (the approach and the halt's frame, the leads cut along every Skimmer lane, the relay's and the hut's floes, brash in the margins and rubble where the walkers cross), the dark arctic water drifting on `deep`, the exit's open floes, the loose ice and the mortars' floe drifting on paths, the fog (light, medium) and the whiteout (look a) as the atmosphere's banks; composites with stand-ins for the units; the GIF the ease into the halt and the whiteout's ramp | proposed |
| [concept/level-13-capture-final-r35-a.png](concept/level-13-capture-final-r35-a.png), [.mp4](concept/level-13-capture-final-r35-a.mp4) | Capture of the game (medium, the balance plan's L13 fit with Rook, no `--invulnerable`): 193 / 208 kills, the relay at 54 % (the secondary met), secret 1 / 1, A, 2,776 credits; sixteen frames (three from shorter runs: easy's whiteout, the damage smoke, the destroyed relay) and the whole run with sound ([recipe](concept/prompts.md#level-13-capture-final-r35-a)) | proposed |
| [concept/level-13-capture-fixes-r35-a.png](concept/level-13-capture-fixes-r35-a.png), [.mp4](concept/level-13-capture-fixes-r35-a.mp4) | Supplement to the capture, the fixes made after it (45 s clip with sound + sheet, [notes](concept/prompts.md#level-13-capture-fixes-r35-a)): the t=20 Skimmers on their leads (a run without firing: two hulls touch a floe edge, t 24.5 and 27.5), the mortars' burst riding their floe (t 77), the pale fog bank against the capture's grey one (t 145), the whiteout (176–186), the end's chord (218.05–220.65) and jingle (from 220.65) without overlap | proposed |
| [concept/level-13-capture-leads-r35-a.png](concept/level-13-capture-leads-r35-a.png), [.mp4](concept/level-13-capture-leads-r35-a.mp4) | Supplementary clip after G5c's wider leads (step CF; 22 s with sound, the level time burnt in, and a sheet; no firing, `--invulnerable`): the t=20 Skimmers through their whole leads, every hull on open water (a 10 fps scan, 2× insets of the nearest ones), and the halt frame: the relay's floe in brash-strewn water, small floes at the side edges ([recipe](concept/prompts.md#level-13-capture-leads-r35-a)) | proposed |

## Implementation

All items below are **M5 part G** (the steps are those of the gap analysis's work plan).

- [x] Timed halt: a `speed: 0` section without an arena (only with a `defend` primary), the clock
      running, the 2 s eases (M5 part G, steps G3 and G4) — simulation in G3 (`Sortie.timedHaltSpeed`,
      `Sortie.scrollAt`, `LevelScript.Section.rampSeconds`, `TimedHaltTest`); the data's `ramp`, the eased
      `LevelData.scrollAt` and the validator's rules in G4 (`PartGChecks`, `PartGLevelKeysTest`)
- [x] `defend` primary on the escort's machinery: the relay `follows: ground`, integrity (tuned: 240 /
      340 / 520), no regeneration, hit flash, smoke below 50 %, dark at 0, mission failed, no primary credits
      (M5 part G, steps G3, G4 and G6) — simulation in G3 (`LevelScript.Defend`, `Convoy`'s defend mode,
      `DefendTest`), the data and loader in G4 (`objectives.defend`, `PartGRules.defend`, `Level13Test`);
      the presentation in G6 (`RelayLooks`: the state frames, the hit flash, smoke below 50 %, fire below
      25 %, dark with its smoke column at 0, the lights' sequence at the sync's end; `Level13LooksTest`)
- [x] Hook modes `alternate`, `in-arc` and `always` with the exact rules above, per kind and per
      difficulty (easy: Stingers unhooked); popcorn never hooked (M5 part G, steps G3 and G4) — simulation
      in G3 (`LevelScript.HookMode`, `EnemyForce`, `HookModesTest`); the data's `targeted_by` with its easy
      change in G4 (`Level13Test.theRelayIsDefended`)
- [x] Skimmer lane walker with the weave, 16 headings, entering from any edge (M5 part G, G3, G4) —
      simulation in G3 (`WaveSpec.lanes`, `SkimmerTest`), the waves' `lanes` in G4
- [x] `cross` formation (n per edge, each on its own lane) with edge warnings on every edge (M5 part G, G3;
      `WaveSpec.Formation.CROSS`, `CrossTest`)
- [x] A ground unit entering at a side edge with a drift (the mortar floe), the circle's centre `at`,
      hard's Stinger pairs (M5 part G, G3, G4) — simulation in G3 (`LevelScript.SideEntry`,
      `WaveSpec.at` on a circle, `WaveSpec.pairSeconds`, `CrossTest`); the data's `side`, `at` and `pairs`
      in G4
- [x] Wave script matches *Waves*: ≥ 50 units a minute at medium (`DensityTest`: 54.3), no empty screen
      over 3 s on every difficulty but the two allowed pauses (`PacingTest`), edge warnings of 3 s on all
      side and rear waves, also in the whiteout (the simulation's, every `from` but the front) (M5 part G, G4)
- [x] Secondary `integrity` (failed at once below the share, met at t=218) and the threshold events
      (M5 part G, G3, G4) — simulation in G3 (`Secondary.integrity`, `CueTrigger.ALLY_BELOW`,
      `DefendTest`); the data's `integrity` and `ally-below` with `below` in G4
- [x] Integrity tuned with the defend autopilot over several seeds (G2 = a) and the measured outcome per
      difficulty recorded (M5 part G, G4; `BalanceTest.theAutopilotKeepsTheRelay`, see *Nansen Relay*)
- [x] Water by unit: only a burst on a Skimmer and its death use the water splash (G5 = a) (M5 part G, G3, G6;
      `water_splash: naval-only`, `LevelScreen.splashes`: a burst on a unit with its own water layer; the
      Skimmer's sub body, wake and flip-and-sink by heading, `NavalLooks`)
- [ ] Production backdrop (five sections, the fixed halt frame with leads cut along the lanes, loose drifting
      ice, fog from t=140, the whiteout banks and snow on the real clock, easy's 6 s whiteout peak, grid
      online; it replaces the data's placeholder `backdrop` block, Level 12's sea); the relay's
      production sprite and lights; the Skimmer's production sprite; the drone and the hut (M5 part G, G2, G5, G6)
      — the backdrop in G5 (`tools/art/backdrop_l13.py`, the data's `backdrop` block and the sections'
      `tiles`; see *Concept art*): the ground one unique strip of four pieces with the halt's frame and
      the leads cut along every lane, the drifting water, the loose ice and the mortars' floe on paths,
      fog light/medium and the whiteout (look a) as the atmosphere's banks; the snow, the banks' roll on
      the real clock and easy's 6 s peak are G6's
- [ ] Research hut secret (110 in Act 1 terms) and the two supply drones (a presentation arc releasing the
      patch at a point) (M5 part G, G3, G4, G5, G6) — the point drop in the simulation done in G3
      (`LevelScript.PlacedPickup` with a y, `SimEvents.Type.PICKUP_PLACED`); the hut trigger, its secret and
      the `drops` with the drone's sprite in the data, the hut's and the drone's sprites in `assets/` in G4;
      the drone's arc in G6 (`DroneLooks`: 4 s from the relay's helipad to the drop, the patch in its
      bay, back with its clamps open, the sides alternating; its launch sound as the arc starts); the
      hut is drawn as a trigger with its beacon glow (the generic trigger looks)
- [x] HUD tracker: `RELAY` with the integrity bar and its 50 % tick, the sync countdown, the secondary's
      line (M5 part G, G6; `MissionPanel`, `PartGHudTest`), the debrief's `RELAY n %` row
      (`LevelScreenPartGTest`)
- [ ] Radio voiced, every timed line at most 1 s late (`RadioTimelineTest`), the integrity events, Nansen
      cast in round 35 (M5 part G, G4, G6) — the cast speakers' 15 lines rendered in G6 (no voiced timed
      line late, none moved); Nansen's lines play as text until round 35 casts him
- [x] Music: "Homefront" from section 2, the full mix in section 4, the jingle after grid online's chord
      (the fade from t=218, the jingle at t=220.6; step G8) and no debrief
      jingle; the arctic ambience (M5 part G, G6; `Level13SoundsTest`) — its +3 dB in the whiteout and
      easy's 6 s peak read the section peak's `ambience_db` and `easy` (code in G6; the data's keys are
      the main agent's merge)
- [ ] Threat profile (the defend exception stated), Varga's four intel lines, Rook's teaser, two briefing
      pages with their images, the L3 strip's defend marks and `IntelPanelLayoutTest` (M5 part G, G4, G5;
      the strip's marks done in G6: `Intel.marks`, `PartGHudTest`; the two images rendered in G5, set as the pages'
      `image` once the texts fit four lines, `BriefingLayoutTest`)
- [x] Typical haul at medium within ±5 % of budget(13) = 1,577 with the level's `bounty_scale`
      (`TypicalHaulTest`: 1,606 at 0.67, +1.8 %) (M5 part G, G4)
- [x] Easy/hard variations as in *Difficulty notes* (easy's 6 s whiteout peak is presentation: the
      backdrop item); `Level13Test`, `PartGLevelKeysTest`, `PartGUnitsDataTest` (M5 part G, G4)
- [ ] Captures and round 35 (M5 part G, G7 and G8)

## Open questions

- Everything below was decided **as recommended while the user is on vacation** and is **pending the
  user's review** in the combined concept round at the end of M5: whether a Stinger should die on the
  relay (a kamikaze) instead of flying on (G1); the measured numbers and the final integrity per
  difficulty (G2); whether ≈ 190 units in 3:55 from all edges feels right for density 4 (G3); the
  leads and the drifting ice in the capture (G4); the drone's look (G6); the whiteout's look (G7).

## Decisions

- 2026-10-01: Promoted from the Act 2 roster to a draft level document.
- 2026-10-01: Primary `defend`: the relay destroyed means mission failed; failing the secondary
  objective only loses its bonus (user decision).
- 2026-10-01: The relay's spec moved to [allies](../../../allies/README.md#nansen-relay). Open question resolved: the per-unit relay-targeting overrides are replaced by the general target-the-objective hook in the enemies README, configured here as a small table.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-11: M5 part G, step G1a: reworked for the M4/M5 rules, so `design: review`. **The user is on
  vacation: the main agent took every recommendation of the gap analysis; all are pending the user's
  review** in the one combined concept round at the end of M5.
- 2026-10-11: **G1 = a** (recommended; pending the user's review) the hook modes made exact on the
  relay-aimed rule: `alternate` (every second attack, the relay closer than the ship, a burst is one
  attack), `in-arc` (the fan centred on the relay within ±45° of the facing), `always` (the Stinger's
  dive and fan at the relay, a single 10-point hit crossing the footprint, then it flies on; the
  mortar's marker, direct hit and lob on the relay). Rejected: b (everything hooked that crosses the
  relay hurts it: far more damage, no counterplay) and c (only aimed shots follow the hook: the divers'
  idea lost).
- 2026-10-11: **G2 = a** (recommended; pending the user's review) the waves kept and the integrity tuned
  per difficulty with the defend autopilot (medium never loses, ≥ 50 % in about half the seeds; easy
  nearly always; hard may lose the secondary, never the relay); measured numbers recorded here later.
  Rejected: b (change the approved wave table) and c (accept what is measured).
- 2026-10-11: **G3 = a** (recommended; pending the user's review) halted time counts as scroll time;
  ≈ 196 units, the extra 114 unhooked popcorn from all edges; no rule change. Rejected: b (a lower
  minimum for `defend` levels) and c (more hooked Skimmers).
- 2026-10-11: **G4 = a** (recommended; pending the user's review) Skimmers are lane walkers on
  ground-space lanes with the weave; the leads and the relay's floe are fixed in the halt, loose ice
  drifts. Rejected: b (flight paths: boats would cross the ice) and c (drifting leads).
- 2026-10-11: **G5 = a** (recommended; pending the user's review) `water: true` for the whole field in
  the simulation, the water splash by unit (a level flag). Rejected: b (a lead mask) and c
  (`water: false`).
- 2026-10-11: **G6 = a** (recommended; pending the user's review) the supply drone is presentation only
  and releases an ordinary patch at the arc's top. Rejected: b (a carried patch) and c (no drone).
- 2026-10-11: **G7 = a** (recommended; pending the user's review) the whiteout banks hide ground units
  at the edges, air stays visible. Rejected: b (a veil dimming air units) and c (snow only).
- 2026-10-11: **G8 = a** (recommended; pending the user's review) the jingle plays once at t=218, the
  debrief without it. Rejected: b (twice) and c (none).
- 2026-10-11: **G9 = a** (recommended; pending the user's review) round 35: a/b for the relay, the
  whiteout peak, three sounds and Nansen; production straight away for the backdrop and the Skimmer;
  one version for the drone and the hut.
- 2026-10-11: Stated defaults of the gap analysis (all pending the user's review): the halt's 2 s eases;
  hit box 80×80; Act 1 terms with a `bounty_scale` (estimate 0.67), the hut 110 and the secondary 95;
  no act HP factor for the Skimmer; Scuttler lanes ending 100–140 px short of the relay; the mortar
  floe from the left at t=66; the circle centred above the relay; the drones' patches at (240, 380) at
  t≈162 and t≈202; the radio retimed to the 1 s rule with Rook's t=82 and t=122 lines dropped; Varga's
  four intel lines; the HUD tracker; the fog from t=140; the threat profile's defend exception; Rook
  keeps the Autocannon; L13's units in L13's own atlas.
- 2026-10-11: M5 part G, step G3 (the simulation; **pending the user's review**, the user is on vacation):
  the timed halt eases by smoothstep on the level clock, the ramp read from the halted section (default
  2 s: down over the last 2 s before it, up over the first 2 s after it); the approach then scrolls
  ≈ 5,460 px to the halt and the exit ≈ 1,960 px. The defend has a start and a duration (t=40 + 178 s:
  the synchronisation completes at t=218, when the integrity secondary is judged; the scroll resumes at
  t=220); the relay stands at its place at the start and is safe once the synchronisation is over
  (nothing aims at it or hurts it); `--invulnerable` keeps a lost relay from failing the level, as a lost
  escort. Under `always` the Stinger's fan keeps its stat block's timing (0.6 s into the dive), so it fires
  from below the relay back up at it, which the player can body-block (rejected: firing as it crosses
  the relay, unblockable); its dive's 10 is dealt when its hit box crosses the relay's 80×80 hit box. A
  relay-aimed lob also hurts a ship inside its impact circle, as any lob. `in-arc` uses the walker's
  `front_arc` (the Scuttler's 45°); `alternate` compares the distances centre to centre and decides at a
  burst's first shot. A column's pair flies 50 px apart. The sim enforces only the structural rules (no
  timed halt with holds or an arena, no defend beside an escort); "a timed halt only with a defend" and
  "nothing enters at the top edge in the halt" are the validator's (G4, `LevelScript.timedHaltAt`).
- 2026-10-11: The draft's errors corrected: the relay at play-field x = 240 (not "480 in screen
  coordinates"), entering at t≈37 (not 34), the edge warning 3 s (not 1.5 s), the budget 1,577 (not
  the perfect-run 2,252), the hut's 180 and the secondary's 150 are the act-scaled values of 110 and 95.
- 2026-10-12: M5 part G, step G4 (the content step; **pending the user's review**, the user is on
  vacation): the level's [data.yaml](data.yaml) and the marked tables rendered from it. The relay's
  integrity tuned per G2 = a with the guarding autopilot over 8 seeds: **240 medium / 340 easy / 520 hard**
  (the draft's 600 / 900 / 450; see *Nansen Relay* for the measurements and why hard's is higher than
  medium's). The popcorn reworked for the pacing rule with the plan's fit: streams of 4 at 2–2.5 s every
  3–5 s, a stream at t=24 after the introduction, the introduction's three Skimmers on easy too; **208
  units** (54 a minute) instead of the plan's 196; the rear popcorn as Skitter line abreasts and the t=140
  rear stream as a snake on a path (only chasers stream from the rear), the t=200 rear Needlers as a line
  abreast (a V-wing enters only from the front). The typical haul 1,606 at `bounty_scale` 0.67 (+1.8 %).
  Recommended traits `spread` and `rear` (`side` in the notes: three do not fit the intel panel). The
  secret's radio line is Varga's (new). Nansen's lines use the generic CDF portrait. The briefing pages
  have no image until step G5 renders them; the backdrop is a placeholder (Level 12's sea, `images:
  level-12`) until G5's arctic one. Rejected: three recommended traits (the panel), the draft's popcorn
  timing (the plan fit clears it with up to 7.5 s empty).
- 2026-10-12: M5 part G, step G6 (the game step; **pending the user's review**, the user is on
  vacation): the relay drawn in variant a (relay.py; state by integrity: smoke below 50 %, fire below
  25 %, dark at 0 with a smoke column of puffs every 0.15 s rising 42 px/s and drifting 16 px/s; a hit
  flashes it white). Grid online at the synchronisation's end, **t=218** (the README's "t=220" read as
  the sync's completion, so the lights, their sound and Nansen's line coincide): the lights' sequence
  over 2 s, then their steady loop; the jingle after the theme's 1 s fade (t=219). The supply drone's
  arc: 4 s from the helipad (the relay's pivot) to the drop's point, swinging 110 px out to one side (the
  first drone right, the second left) and arriving at the arc's top, then 4 s back with its clamps open
  on the other side; it flies only while the relay stands. The Skimmer: its plain body per heading in
  the sub pass, its wake per heading on the surface, its tail beat at 10 fps (the organic rate; the art
  suggested 8), its death a small burst over the water splash and the flip-and-sink at the nearest of
  8 headings (6 steps a frame). On the ice the water splash goes by unit: a burst within 8 px of a
  Skimmer's hit box is the water splash, everything else the ground burst. The arctic setting snows on
  high-air by the atmosphere (none in light, 30 % in medium, full in the heavy whiteout, ramping with
  the backdrop), on the real clock, at most 40 % per flake. Easy's whiteout (6 s, 179–185) and the
  wind's +3 dB come from two new optional keys of a section peak, `easy` (s) and `ambience_db`; the
  data line `peak: {atmosphere: heavy, from: 176, to: 188, easy: 6, ambience_db: 3}` is the main agent's
  merge (the data was being edited by step G5).
- 2026-10-12: M5 part G, step G5 (the backdrop and the briefing images, straight to production, G9 = a;
  all pending the user's review in round 35): **the ground is one unique strip** (four `ground` pieces
  of 480×1,536, the approach and the halt's frame in one render) instead of a repeating ice tile: in a
  timed halt every later section's tile set begins at the halt's top edge (ground 6,000), so the halt
  shows section 1's ground, and a tile would have repeated the halt's leads through the approach (across
  the hut's floe and the introduction's lead). The leads are cut along **every** Skimmer lane (the
  simulation's Catmull-Rom curves, 2 × 44 px: the 40 px weave and most of the hull), so the halt's frame
  is the relay's floe in a ring of leads with floes beyond them; the Scuttlers' paths run in the west and
  east leads' margins and cross the leads (the data's), so the leads are choked with brash rubble where
  they cross. The water is the `deep` tile drifting 3 px/s left on the real clock (the leads' water moves
  in the halt); the loose ice is one `loose-ice` piece with 8 headings (a different floe per direction)
  on straight 6 px/s paths along the leads' outer margins (outside the lanes is ice: they drift in the
  leads), and the mortars' floe is its east-heading frame on a 10 px/s path in step with the simulation's
  side entry; that keeps the motion budget's two elements (the water's drift and the loose ice). The fog
  (light ~10 %, medium ~20 % at the edges) and the whiteout (look a's banks, ~30 % opaque at the edges, the
  centre clear) are the atmosphere's static banks, cross-faded over the 4 s ramp; their real-clock roll,
  the snow and easy's 6 s peak are the game's (G6). The exit has its own `open-floes`, open water where it
  begins. Rejected: an opaque halt piece over a repeating tile (its water could not drift and slides
  against the deep layer while it scrolls in).
- 2026-10-12: M5 part G, step G7, the capture (round 35's [level-13-capture-final-r35-a](concept/level-13-capture-final-r35-a.png);
  the user is on vacation, so **pending the user's review**). Fixed in the game: the Skimmers were drawn
  as flyers (no wake, a drop shadow, over the whiteout banks): a lane walker is now on the ground units'
  depth; the relay's damage smoke below 50 % (three static 16 px puffs, unreadable on the ice) now rises
  and drifts off each smoke point at 2× as tools/art/relay.py's review loop; the drone launch +2 dB.
  Seen and left for review: a Skimmer of the t = 20 introduction crossing a floe instead of its lead; the
  mortars' wreck stays at the left edge while their floe drifts on; grid online's chord lands on the
  jingle's first second; the debrief comes ≈ 16 s after the level's end (t 235) behind two radio lines.
- 2026-10-12: M5 part G, step G5b, the backdrop's fixes after the capture (**pending the user's review**).
  The t = 20 Skimmer crossing a floe: the sim lays a lane on the ground at each unit's *entry*
  (`Formations.lanes`: a convoy's units 1.5 s apart), the generator at the wave's time, so the second and
  third units' lanes lay 210 and 420 px further up the ice than their leads (32 % and 38 % of their
  points on ice); `--check` missed it because it only counted lane points on screen at the wave's time
  (the introduction's lane starts above the top edge). Now every unit's lead is cut at its entry and
  `--check` follows each unit's run (centre and weave, per wave). The lead's rough edge no longer narrows
  below 42 px, so the weaving hull stays in it (97–100 % on water per wave; the ice a little thinner: the
  halt's frame 29 % ice instead of 32 %). The fog banks read as grey smudges (r03's bank shaded their thin
  edges darker than the ice, two hard alpha steps): now pale, soft-edged fog lighter than the ice it covers
  (`--check`: +16 / +25 luminance over the ice, wisps / banks). The mortars' floe path matches the sim's
  side entry (centre x −22 at t = 66, 10 px/s); their wreck staying at the left edge is game-side.
- 2026-10-12: M5 part G, step G5c, the Level 13 lead margin (**pending the user's review**). The
  supplementary clip ([level-13-capture-fixes-r35-a](concept/level-13-capture-fixes-r35-a.png)) still
  showed the t=20 Skimmers' hulls on floe edges (t 24.5, 27.5): `--check` tested only the weaving hull's
  centre, and a lead 44 px either side (rough edge >= 42 px) leaves the 40 px weave 2 px for a hull
  14.5 px wide either side of its centre. `--check` now tests the hull's full width (the weave's centre
  +- 15 px across the lane, all three on water) and the leads are 58 px either side, the rough edge never
  narrower than 57 px, the margins' brash only in the bays beyond the hulls' reach. Every hull sample is
  on water except 131 of 3,780 on the rubble where a Scuttler's path crosses a lead (G4's choke, kept).
  The ice is thinner: the approach 38 % (was about 41 %), the halt's frame 20 % (was 29 %): the halt's
  lanes leave only 24 % of the field beyond the hulls' reach, so the halt is the relay's floe in brash-
  strewn water with floes at the edges; the relay's and the hut's floes are unchanged (100 % ice).
- 2026-10-12: M5 part G, step G8, the game's fixes after the capture (**pending the user's review**).
  **Grid online and the jingle**: the chord (2.6 s from t=218) sat on the jingle's first second, both
  near −17 dBFS; the music block's `jingle` is now `{t: 218, fade: 2.6}`, so the theme fades with the
  chord and the jingle starts as it ends (t=220.6). **The mortars' wreck**: a ground unit that drifts on
  top of the scroll (the mortars' floe, 10 px/s) now hands its drift to the game with its death (a new
  simulation event, `WRECK_DRIFT`, presentation only), and its death burst and remains drift on with the
  floe instead of staying at the left edge where it was shot.
