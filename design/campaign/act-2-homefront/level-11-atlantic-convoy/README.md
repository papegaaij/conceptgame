---
title: Level 11 – Atlantic Convoy
design: approved
implementation: done
art: final
depends-on: [../../../enemies/bosses/harbour-kraken, ../../../enemies/naval/driftjelly, ../../../enemies/naval/reef-spitter, ../../../allies, ../../../player/weapons/torpedo-pod, ../../../player/wingmen]
updated: 2026-10-09
---

# Level 11 – Atlantic Convoy

## Summary

The first sea level and the Act 2 mid-boss. Convoy **Atlas-Seven** carries reactor parts for
the Arctic relays across a grey Atlantic that the Vrell have seeded with drifting
[Driftjellies](../../../enemies/naval/driftjelly/README.md) and reef-grown
[Reef Spitter](../../../enemies/naval/reef-spitter/README.md) gun rafts. The level introduces the
naval surface and the `sub` layer: jellies swap between the surface and the water below it, where
only the optional [Torpedo Pod](../../../player/weapons/torpedo-pod/README.md) reaches them. At the
overrun fusion platform Tiamat the scroll halts for the mid-boss, the
[Harbour Kraken](../../../enemies/bosses/harbour-kraken/README.md), whose arms slam lanes where
three cargo ships hold station. 160 s of scroll at 140 px/s, the mid-boss (tuned to about 60 s)
and a 15 s exit.

**Status (M5 part E, step E3a).** This document states the design decided on 2026-10-08 (user
decisions E1–E10 and the 28 stated defaults of the part's gap check, see *Decisions*) and is the
ticket for the later steps. The level's [`data.yaml`](data.yaml) (waves, ground targets, convoy,
radio, budget, threat profile, briefing, backdrop, music) is written; the marked tables below are
rendered from it (`tools/sync_tables.py`).

## Briefing

Two pages, as Levels 08–10, each **one screen** (`BriefingLayoutTest`): Okafor's mission (the
convoy and Tiamat), then Varga's warning about what lives in the water. Draft texts, for review in
concept round 33:

<!-- data: briefing -->
> **Commander Okafor:** "The Arctic relays are running on reserve power, Lancer. Convoy
> Atlas-Seven carries their reactor parts across the Atlantic: three cargo hulls and a frigate.
> Something large has been tracked near Platform Tiamat, right on their route. Get them to open
> water."
>
> **Dr. Varga:** "The Vrell have seeded the sea lanes. Jellies drift on the surface and below
> it, and gun rafts grow on the reefs. Our guns can't touch what's under the waves. Torpedo pods
> can, if you buy them."
<!-- /data -->

*Images* (production art straight away, user decision E9 = a; [briefing images](../../../ui/briefing/README.md)):
Okafor's page `level-11-convoy-route` (the Atlantic route from above: the convoy's four hulls, the
seeded sea lanes, the reef line, Platform Tiamat marked), Varga's `level-11-sub-scan` (a Driftjelly
on the surface and below it, cut at the waterline, and a torpedo's run under the surface); the
level's data names them as each page's `image`.

*Hangar teaser* (shop screen before L11; Rook, `LT. K. TANAKA / AEGIS TWO`, not voiced, as Level
10's):

<!-- data: teaser -->
> **Rook:** "Cargo ships to mind and jellyfish that shoot back. Torpedo pods are in the shop, if
> you fancy fishing."
<!-- /data -->

## Threat profile

Generated from the level data's `threat_profile`. The directions are counted by each wave's `from`
(the intel's rule); the jelly fields, the raft nests and the Kraken all come from ahead.

<!-- data: threat-profile -->
| Field | Value |
|---|---|
| Dominant layers | `ground` (naval surface: rafts, surfaced jellies, the Kraken's arms and head when up), `sub` (submerged jellies, the Kraken below, a sunken pod), `air` |
| Attack directions | front 98% · sides 2% |
| Density | 3 |
| Recommended traits | `spread`, `anti-ground`; `anti-sub` optional (submerged jellies, the sunken pod, the Kraken's head in phase 1), not `required` |
| Hazards | Driftjelly fields (proximity rings on and under the surface) |
| Boss / mid-boss | Mid-boss [Harbour Kraken](../../../enemies/bosses/harbour-kraken/README.md) |
| Sensor-suite detail | none: open ocean, `ground` + `sub` + `air`, front · L1: + directions, density 3, hazard "drifting organisms", the objective `CONVOY TO OPEN WATER` · L2: + Driftjelly (new), Reef Spitter (new), Needler, Stinger, Skitter and Mote Swarm portraits; the mid-boss silhouette "unknown, large, submerged" · L3: + `spread` and `anti-ground` highlighted (`anti-sub` marked optional), the wave strip marking the mid-boss at ≈ 160 s, 1 secret |
<!-- /data -->

*Varga's intel line* per sensor-suite level (the data's `varga`; draft texts for round 33, fitting
the intel panel is checked by `IntelPanelLayoutTest`):

| Sensor suite | Line |
|---|---|
| none | "Open Atlantic, Lancer. Something rides the current out there, on the water and under it." |
| L1 | "Mostly from ahead. Drifting organisms on the sea lanes: don't loiter over them." |
| L2 | "New: Driftjellies and Reef Spitters. And something huge." |
| L3 | "Anti-ground for rafts. Torpedoes for the sunken cache." |

## Objective

- **Primary** `reach-end`: reach open water. The scroll stays halted at Tiamat until the Kraken is
  destroyed. The convoy is **not** the primary objective: losing ships never fails the mission
  (there is no mission-failed line).
- **Secondary** "Convoy afloat" (user decision E8 = a, all or nothing): no cargo ship sunk. It is
  decided at the Kraken's death: **met** when the Kraken dies with all three cargo ships afloat
  (damaged or not), **failed** at once when one sinks. Pays **100** credits in Act 1 terms (160 in
  Act 2, the credit factor 1.6); the typical haul counts it at the economy's secondary share. The
  new secondary kind `afloat` ([schemas](../../../tech/architecture/README.md#data-file-schemas)).

### The convoy

Three [cargo ships](../../../allies/README.md#convoy-cargo-ship), **Halvorsen**, **Mbeki** and
**Saint-Laurent**, and the [escort frigate](../../../allies/README.md#escort-frigate) **CDFS
Ruyter**; their specs (layer, damage, sinking, frigate flak) are in the allies part. The level gives
the stations and the arena lanes in a `convoy` block of its own, outside the objectives (the
[schemas](../../../tech/architecture/README.md#data-file-schemas)); first values for the data step:

- **Stations** (default): the four hulls hold screen-space stations in the lower half at the scroll
  speed, as in the chosen ocean scene, so the sea streams past them; no lane sway, wakes only.
  Halvorsen (130, 380), Mbeki (240, 350), Saint-Laurent (350, 380), Ruyter trailing at (240, 480)
  (px from the play field's left edge, px below its top edge; the ships bow up). Nothing hurts them
  before the arena; the player and Rook fly over them.
- **At the halt** (default): the cargo ships glide to their lanes over **≈ 3 s**: Halvorsen to lane
  1, Mbeki to lane 2, Saint-Laurent to lane 4, each centred in its lane at y ≈ 450 (the lower band,
  y ≈ 390–510); the frigate drops back off the bottom edge over the same 3 s and stays out of the
  arena. The glide runs on the simulation's real steps (the level clock is halted).
- **In the fight** only the Kraken's slams hurt a cargo ship: a slam in its lane is one hit; it
  survives three (smoke and a list from the first) and sinks on the fourth (easy: on the fifth;
  hard: on the second), with a foam ring, the `ally-lost` line naming it (tuned 2026-10-09, see
  *Boss / mid-boss*). Lane choice and slams: the
  [Kraken's behaviour](../../../enemies/bosses/harbour-kraken/README.md#behaviour).
- **After the Kraken** (section 6): the convoy **holds clear of Platform Tiamat** while its deck
  (x 136–344) scrolls down through the play field: Halvorsen and Saint-Laurent hold in lanes 1 and 4
  (clear of the deck), Mbeki drops back off the bottom edge from lane 2 (its `hold: [180, 620]`) and
  the frigate stays away, until the sea has scrolled the convoy's `hold_clear` of **552 px** (the
  deck's top edge, 538 px above the bottom edge at the halt, has passed it; ≈ 3.9 s at 140 px/s;
  520 and 506 before round 33 raised the platform).
  Then the ships still afloat glide back to their stations over ≈ 3 s and the frigate returns from
  the bottom edge to its own; the convoy steams north out of the level (`Level11Test`).
- **HUD** (default): a one-line [objective tracker](../../../ui/hud/README.md#left-panel-mission),
  `CONVOY` with three ship pips (green; amber after a ship's first slam; a red flash then dark when
  sunk), turning `DONE` or `FAILED` at the Kraken's death; the debrief's secondary row
  `HULLS AFLOAT n / 3`.
- **Retry** (default): the boss checkpoint (the arena halt, the sting) keeps each ship's state
  (afloat, damaged, sunk) among the objective tallies, so *Retry from boss* restores the ships as
  they were when the fight began ([retry](../../../systems/retry/README.md)).
- **`--invulnerable`** does not change the ships or the secondary's logic.

## Layout

Ground scroll speed **140 px/s** (the chosen ocean scene's slower swell). The level is over water
throughout: its data sets **`water: true`** (user decision E1 = a), which is what makes the Torpedo
Pod run ([torpedo pod](../../../player/weapons/torpedo-pod/README.md#behaviour)) and every kill or
landing blast on the play field a water one. The
[water rules](../../../art-direction/README.md#animation-rules) apply to everything that meets the
surface (foam collars, ripple trains, submerged parts visible and connected, nothing pops in), and
flyers cast their shadows onto the sea (default). Motion budget: the sea and the wakes are the two
strong elements; mist, smoke and whitecaps stay faint.

**Timeline.** The launch takes the first 5 s. Sections 1–4 scroll 160 s (22,400 px). Section 5 is
the **arena** (`arena: true`, `speed: 0`, new in M5 part E), t = 160–161: the scroll eases to a
halt over its first second (68.8 px at 140 px/s, so the halt scroll is 22,468.8 px) with Tiamat's
centre 78 px below the top edge (110 until round 33: raised so the Kraken's head surfaces in open
water off the deck's south edge, the deck at y 2–154), and the level clock halts at its end (t = 161) until the Kraken
dies; section 6 then starts and scrolls 15 s (2,100 px), with a lighter overcast from t=170. About
**236 s** with a 60 s fight. Because the level clock halts at the arena, everything in the fight
runs on the simulation's real steps (the ships' glide, the jelly swaps, the lane cycle, the arena's
jelly field) and the arena's radio lines are events, not times. The backdrop's Platform Tiamat is
placed at the halt scroll (its centre 22,898.8 px up the ground; `t` 162.635 in the data, by the
data's scroll, which counts the arena at speed 0).

<!-- data: level-sections -->
| Section | t (s) | Scroll (px) | Speed (px/s) | Atmosphere | Layers and content | Purpose |
|---|---|---|---|---|---|---|
| 1. Convoy Muster | 0–20 | 0–2,800 | 140 | light | `deep`: slate swell with a slow colour field. `ground`: the convoy at its stations, wakes and prop-wash. `low-air`: sea-mist banks (~15%). `high-air`: thin wisps. | Meet the convoy; the first popcorn from t=7. |
| 2. Jelly Fields | 20–65 | 2,800–9,100 | 140 | light | `ground`: Driftjelly fields drifting in the current, surfaced jellies cut at the waterline; Vrell spawn slicks. `sub`: submerged jellies under the tint pass; at t≈55 a huge dark shape (≈ 400 px) slides under the convoy and fades into the deep (the Kraken, foreshadowing, scenery). | Driftjellies introduced alone first; the `sub` layer seen. |
| 3. Reef Line | 65–110 | 9,100–15,400 | 140 | medium | `ground`: violet-veined Vrell reef growths breaking the surface in lines, with foam collars; Reef Spitter rafts bobbing on kelp. `sub`: reef roots fading into the deep, a CDF supply pod snagged on one (the secret). `low-air`: mist banks (~22%). | Reef Spitters introduced as a lone nest of 3, then combined with a jelly field. |
| 4. Tiamat Approach | 110–160 | 15,400–22,400 | 140 | light | `ground`: debris and floating cargo containers from a burning freighter (smoke at half particle speed, per the motion budget); the overrun fusion platform Tiamat enters at the top at the end, gripped by the Kraken's armoured arms. `sub`: the Kraken's mantle shadow under the platform. | Mixed waves; the last Reef Spitter nest; the flotsam pickups. |
| 5. Harbour Kraken | 160–161 | 22,400–22,400 | 0 | clear | `ground`: platform Tiamat at the top of the play field, four lanes of 120 px below it, the three cargo ships in lanes 1, 2 and 4 in the lower band. `sub`: the Kraken's mantle, head and idle arms under the surface. | The mid-boss (≈ 60 s at medium); the scroll stays halted while it lives, and section 6 starts at its death. |
| 6. Open Water | 161–176 | 22,400–24,500 | 140 | clear, light peak 170–176 | `ground`: the convoy steaming north past the abandoned platform. `deep`: the swell under a lighter overcast. | Release; level end. |
<!-- /data -->

## Waves

Enemy definitions are linked per row; formation names come from the
[formation vocabulary](../../../enemies/README.md#formation-vocabulary). The returning units are
`tiny` or `small` (Skitter, Needler, Stinger), so no act HP factor applies (default).

- **`field`** (new, default): a Driftjelly wave is a field of scattered units that enters at the top
  edge with the sea (scroll factor 1) and drifts on the current at the jelly's drift speed (15 px/s);
  each jelly swaps between surfaced and submerged on its own seeded timer (6–10 s), a field starting
  about half submerged ([driftjelly](../../../enemies/naval/driftjelly/README.md)). `swarm` stays
  Level 10's flock.
- **Density and pacing** (step E3a, default): at least **50 enemies a minute** at medium up to the
  arena (`DensityTest`; the arena's jelly field does not count) and no empty screen over **3 s** of
  real time (`PacingTest`, every difficulty). The draft's 71 units over ≈ 156 s (≈ 27 a minute) were
  too few: the table keeps the set pieces (the introductions, the jelly fields, the V-wings, the
  pincer, the ten-Skitter stream, the raft nests) and fills the gaps with Act 2 popcorn, a wave every
  3–3.5 s from t=7: Skitter streams of 4 and snakes of 6, small Mote Swarms of 8 down a side lane or
  across the lower band (cheap: 2 credits a mote, so the haul stays on the budget with a
  `bounty_scale` of 0.6), and a few Needler lines. **254 enemies before the arena at medium, ≈ 98 a
  minute**; two popcorn waves follow the Kraken in section 6. `PacingTest` with the autopilot and the
  plan's fit: no pause over 3 s but the quiet end on every difficulty. No rear waves (front 98 % by
  count, the pincer from the sides). The two "alone" introductions keep a quiet window: no other
  wave enters for 3.5 s after the first jelly field (t=22) or for 3.5 s after the first raft nest
  (t=70), and the popcorn before each leaves the screen first, as Level 10's "safe first sight".
- **The second raft nest** moved from t≈92 to t≈101 (with the t=100 jelly field, as the draft's
  notes say): at t≈92 a raft lay 0.8 s ahead of the sunken pod, where the torpedoes that should free
  the pod locked on to it (a torpedo picks the nearest `sub` or naval target in its cone, and the pod
  is a trigger, not a target).

<!-- data: waves -->
| t (s) | Section | Formation | Enemies (link) | Count | Enter from | Notes |
|---|---|---|---|---|---|---|
| 7 | 1 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (right) | Over the convoy at its stations; the first targets |
| 11.5 | 1 | swarm | [Mote Swarm](../../../enemies/air/mote-swarm/README.md) | 8 | front | |
| 15 | 1 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (left) | |
| 18.5 | 1 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 3 | front (alternating edges) | Gone before the first jelly field |
| 22 | 2 | field | [Driftjelly](../../../enemies/naval/driftjelly/README.md) | 6 | front | **New.** Alone (no other wave until t=25.5); the ring fires only when the ship is within 96 px; about half of them start submerged |
| 25.5 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (left) | |
| 29 | 2 | swarm | [Mote Swarm](../../../enemies/air/mote-swarm/README.md) | 8 | front | |
| 32 | 2 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 5 | front | |
| 36 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | |
| 40 | 2 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 8 | front (right) | Weaves above the jelly field |
| 44 | 2 | swarm | [Mote Swarm](../../../enemies/air/mote-swarm/README.md) | 8 | front | |
| 47 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (left) | |
| 50 | 2 | field | [Driftjelly](../../../enemies/naval/driftjelly/README.md) | 8 | front | Denser field, drifting to the right; easy 6 |
| 54 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | |
| 55.5 | 2 | swarm | [Mote Swarm](../../../enemies/air/mote-swarm/README.md) | 8 | front | Down the left lane as the huge shape slides under the convoy (Rook's line at t=55) |
| 59 | 2 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (right) | |
| 62.5 | 2 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 3 | front | |
| 66 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (left) | Gone before the first raft nest |
| 73.5 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | After the lone raft nest's quiet window |
| 77 | 3 | swarm | [Mote Swarm](../../../enemies/air/mote-swarm/README.md) | 8 | front | |
| 80.5 | 3 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (left) | |
| 84 | 3 | V-wing | [Stinger](../../../enemies/air/stinger/README.md) | 4 | front | The leader drops an overdrive |
| 87.5 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (right) | |
| 91 | 3 | swarm | [Mote Swarm](../../../enemies/air/mote-swarm/README.md) | 8 | front | Over the reef line before the sunken pod |
| 94.5 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | Over the sunken pod's reef line |
| 100 | 3 | field | [Driftjelly](../../../enemies/naval/driftjelly/README.md) | 8 | front | Between the reef lines, with the second raft nest, drifting to the left; easy 6 |
| 103.5 | 3 | swarm | [Mote Swarm](../../../enemies/air/mote-swarm/README.md) | 8 | front | |
| 107 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (right) | |
| 110.5 | 4 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (left) | Over the first floating containers |
| 114 | 4 | swarm | [Mote Swarm](../../../enemies/air/mote-swarm/README.md) | 8 | front | |
| 116.5 | 4 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 3 | front (alternating edges) | Over the floating containers |
| 118 | 4 | pincer | [Needler](../../../enemies/air/needler/README.md) | 4 | sides | 2 per side, hold 4 s; Rook's flank bark |
| 121.5 | 4 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | |
| 125 | 4 | swarm | [Mote Swarm](../../../enemies/air/mote-swarm/README.md) | 8 | front | |
| 130.5 | 4 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 3 | front | Beside the burning freighter |
| 134 | 4 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (right) | |
| 137.5 | 4 | swarm | [Mote Swarm](../../../enemies/air/mote-swarm/README.md) | 8 | front | |
| 140 | 4 | field | [Driftjelly](../../../enemies/naval/driftjelly/README.md) | 6 | front | Among the flotsam |
| 143.5 | 4 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | |
| 146 | 4 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 10 | front (alternating edges) | |
| 150.5 | 4 | swarm | [Mote Swarm](../../../enemies/air/mote-swarm/README.md) | 8 | front | |
| 154 | 4 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 4 | front | |
| 157 | 4 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (left) | The last wave before Tiamat enters at the top |
| 160 | 5 | mid-boss | [Harbour Kraken](../../../enemies/bosses/harbour-kraken/README.md) | 1 | front (anchored to Platform Tiamat, scrolls in with it) | At the halt (the bar, the sting, the boss checkpoint); releases a Driftjelly field of 6 in lanes 1 and 4 at its first surfacing |
| 163 | 6 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | Over open water, after the Kraken |
| 166.5 | 6 | swarm | [Mote Swarm](../../../enemies/air/mote-swarm/README.md) | 8 | front | The last wave |

Totals: Skitter 110 · Mote Swarm 96 · Driftjelly 28 · Needler 19 · Stinger 4 · Harbour Kraken 1.
<!-- /data -->

The arena's jelly field (6 Driftjellies in lanes 1 and 4) is released by the Kraken at its first
surfacing (a boss release, not a timed wave: the level clock is halted; the spec's "Driftjelly
fields around the arena").

## Ground targets

<!-- data: ground-targets -->
| Section | Target | Effect |
|---|---|---|
| 3 | **New.** A lone [Reef Spitter](../../../enemies/naval/reef-spitter/README.md) nest ×3 (t≈70): no other wave enters until t=73 | Bounty 14 each; the raft sinks when the gun dies |
| 3 | Reef Spitter nest ×4 (t≈101), drifting to the right | With the t=100 jelly field |
| 3 | Snagged CDF supply pod on a reef root (`sub` trigger, t≈94) | 3 torpedo hits (hard 5) or a Smart Bomb free it; floats up as the hidden crate |
| 4 | Floating cargo containers ×8 from the burning freighter (`ground`, 3 HP, t≈112–135) | Each drops a small salvage pickup (10); the last one destroyed drops an armour patch |
| 4 | Reef Spitter nest ×5 across from the burning freighter (t≈128), drifting to the left | Bounty |
| 4 | Reef Spitter nest among the flotsam (t≈140), **hard only**; hard ×3 | Bounty |
<!-- /data -->

The containers keep to the gaps between the convoy's hulls (the hulls at x 130, 240 and 350, 56 px
wide). A raft's 84 px base does not fit a 54 px gap, so the rafts keep to the **flanks** (x 46–58 and
430, their drift on the current included; 2026-10-09): no raft overlaps a hull as it scrolls past
the stations (`Level11Test`). The lone nest and hard's nest take both flanks; the second nest (with
the t=100 jelly field) and the freighter's are columns on the left flank (the freighter and its
smoke fill the right edge; on both flanks the second nest's fans crossing the jelly field cost the
autopilot 8 more armour on hard). The containers
are a destructible `group` of their own (`containers`), new in step E3a: the pickup placed on the
group (`dropped_by: {group: containers, unit: last}`) drops where the last of them is destroyed once
all eight are; the objectives never count it.
Every raft drifts at 10 px/s on the current; a kill on the water splashes and sinks (no crater, no
wreck).

## Hazards

- **Driftjelly fields**: the jellies' proximity rings make flying low over a field costly. Surfaced
  **and** submerged jellies fire (user decision E3 = b); a submerged one shows its quickening pulse
  through the water as the warning and can only be hit by a torpedo or the Smart Bomb until it
  surfaces again (6–10 s).
- No terrain collision; reef growths are ground scenery.

## Secrets and pickups

- **Sunken supply pod** (enters at t≈93.8, on the reef root of the t=96 reef line at x 296): a CDF
  pod snagged on a reef root on the `sub` layer, a trigger that counts **hits**, not HP (default):
  **3 torpedo hits** free it (hard 5, the trigger's `hard: {hits: 5}`; 4 and 6 until round 33), whatever the
  torpedo's level; a Smart Bomb frees it at once. No other weapon reaches it. Freed, it floats up as
  a crate worth **100** credits in Act 1 terms (160 in Act 2). The Salvage scanner's glint shows
  through the water. A reward for buying `anti-sub` early. Its line, Varga: "*A CDF supply pod. Good
  fishing, Lancer.*" (draft). A torpedo fired while the pod is on the screen **seeks it**
  (2026-10-09: a sunken trigger is in the torpedo's target pick); one fired before it enters has
  nothing to seek and runs straight. It is in reach for about 4 s of a pass from the start line, so
  the hits depend on the pilot's timing: a single Torpedo Pod at L1 (0.8 torpedoes a second) lined
  up under it lands the **3** and frees it on medium, whether firing from a second before it enters
  (the first torpedo meets it at the top edge) or out of step; 24 px off the line its seeking
  torpedoes free it too; on hard (5) it takes a pod on each wing (`Level11Test`; round 33, user).
- **Overdrive**: dropped by the t=84 Stinger leader.
- **Armour patch**: from the last floating container destroyed; a shield cell drops when the Kraken
  goes from phase 2 to phase 3 (its spec).

## Radio chatter

Spoken radio ([voice](../../../audio/voice/README.md)), as Levels 08–10. The draft's script is
**retimed to the HUD's 1 s rule** (`RadioTimelineTest`, default): a timed line holds the radio for
the longer of its voice and its text (5 s for the last page plus 3 s for each page before it, typed
at 30 characters a second), then 0.4 s of silence, and starts after the one before it has closed.
The draft clashed at t=1/3, 20/26, 55/56/68 and 150/155/158; now: Atlas Control at t=1 (two pages,
closing ≈ 13.2) and Rook at t=14; Varga at t=21.5 (as the first field enters) and t=34; Rook at
t=55 (the passing shape) and Atlas Control at t=62.5; Varga at t=70.5 (the first raft nest); Atlas
Control at t=110; the Choir at t=140, Varga at t=146.5 and Rook at t=158, all before the halt. The
arena's lines are **events** (the level clock is halted): `first-telegraph` (new: the attempt's
first lane telegraph), `boss-part-destroyed` (new: the first of the named boss parts shot off, here
either slam arm), `ally-hit` (new: each convoy unit's first hit, `{ally}` its name), `ally-lost`,
`boss-destroyed` and `secondary-objective`. The data keeps these times: as text every timed line
starts on time (the last, Rook's at t=158, closes at ≈ 164 on the real clock, in the fight); the
voiced check (`RadioTimelineTest`, Level 11's seven runs with and without Rook) finds no timed
line late with the rendered voices (M5 part E, step E4; Atlas Control, cast in round 33 as MaryAnn,
voiced at its close).

**New speaker** (user decision E10 = a): **Atlas Control**, the CDF officer of Atlas-Seven,
auditioned a/b in concept round 33 as the Kilo Lead and Lifeline were (two new CC0 or public-domain
readers, radio filter b, neutral; the radio portrait `radio-generic-cdf`); `uncast` until the round
picks. `{ally}` in a convoy line becomes the ship's **name** (Halvorsen, Mbeki, Saint-Laurent: three
takes per line, default). **Rook's lines** carry `requires: escort`.

<!-- data: radio -->
| Trigger | Speaker | Line |
|---|---|---|
| t=1 | Generic CDF (Atlas Control) | "Aegis flight, Atlas-Seven. Three hulls of reactor parts for the Arctic grid. We'd like to arrive with all three." |
| t=14 | Rook | "Long way to swim, Atlas. We'll keep you dry." |
| t=21.5 | Varga | "Those jellies drift with the current. Don't hover over them: they pulse when you're close." |
| t=34 | Varga | "Some are under the surface, and they still pulse. Our guns can't reach them there. Torpedoes can." |
| t=55 | Rook | "Lancer... did the sea just move under the convoy?" |
| t=62.5 | Generic CDF (Atlas Control) | "Sonar has a contact. Big. Very big. It's gone deep again." |
| t=70.5 | Varga | "Gun rafts on the reef. Barnacle guns. Anti-ground rounds crack them twice as fast." |
| t=110 | Generic CDF (Atlas Control) | "Platform Tiamat has been silent for six hours. Our route runs right past it." |
| t=140 | The Choir (distorted) | "[the Choir sings]" |
| t=146.5 | Varga | "The contact is rising under the platform. Lancer, it's holding on to it." |
| t=158 | Rook | "Here comes the big one." |
| First lane telegraph (`first-telegraph`) | Okafor | "Watch the churning water. That's where the arms come down." |
| First slam arm severed (`boss-part-destroyed`, the slam arms) | Rook | "Tell the cook we're having calamari." |
| A cargo ship's first hit (`ally-hit`) | Generic CDF (Atlas Control) | "The {ally} is hit! Taking on water, but holding!" |
| A cargo ship sinks (`ally-lost`) | Generic CDF (Atlas Control) | "We've lost the {ally}. Crew in the water. Ruyter is picking them up." |
| Kraken destroyed (`boss-destroyed`) | Varga | "It's letting go of the platform. It's sinking. Good." |
| Secondary objective met | Generic CDF (Atlas Control) | "All three hulls afloat. Drinks are on Atlas, Aegis." |
| Level end, 1–3 ships afloat | Okafor | "Atlas-Seven is through. The Arctic grid gets its parts. Good work, Aegis." |
| Level end, no ship afloat | Okafor | "We lost Atlas-Seven, but the sea lane is open. Come home, Lancer." |
<!-- /data -->

Changed from the draft (our readings, for review in round 33): Varga's t=26 line now says the
submerged jellies still pulse (E3 = b); her t=155 line says "rising" (the head stays down in phase
1); the sinking line names the frigate instead of "the frigate is picking them up" (it has left the
arena); a second level-end line for a convoy lost to the last hull (the cue's `allies`: the cargo
ships afloat). On easy a ship's second hit plays no line (only the first hit and the sinking do).

## Boss / mid-boss

Mid-boss: [Harbour Kraken](../../../enemies/bosses/harbour-kraken/README.md). Its phases, lanes,
HP, attacks and bounty live in the spec. Arena notes for this level:

- The scroll halts with Tiamat at the top of the play field; the four slam lanes are 120 px wide
  (lane 1 at the left edge) and run the full height below the platform. The Kraken is **anchored**:
  it scrolls in with the platform, invulnerable and without its bar, and engages at the halt
  ([schemas](../../../tech/architecture/README.md#data-file-schemas)).
- The cargo ships hold lanes 1, 2 and 4 in the lower band; the left slam arm serves lanes 1–2 and
  the right one lanes 3–4 (user decision E5 = a), so severing the left arm makes Halvorsen's and
  Mbeki's lanes safe and the right arm Saint-Laurent's.
- Players with the optional Torpedo Pod can damage the submerged head in phase 1 (spec).
- The arena's jelly field (6 jellies in lanes 1 and 4) is released at the Kraken's first surfacing.
- The mini-boss sting plays at the halt, when the boss bar appears (default, as the other
  mid-bosses); the boss checkpoint is recorded there ([retry](../../../systems/retry/README.md)).
- The fight is tuned to **≈ 60 s** at medium with the autopilot (E6 = a); its `par` comes from the
  measured fight. In the level (`Level11Test`, the plan's fit with Rook, KrakenTest's five seeds,
  the bar to the kill), the first pilot (eyes first): **62.5 s** mean at medium (56.4–79.0), 84.6 s
  on hard; the arm-first pilot: **55.5 s** at medium, 39.2 s on easy, 98.9 s on hard. Every run
  completes; on hard a pilot worn down in the arena takes several attempts (a retry restarts the
  level), as hard should.
- **The convoy bonus** (user, 2026-10-09: "measure, then tune"). Every slam in the left half strikes
  a ship's lane (Halvorsen's or Mbeki's), so each slam the left arm lies awash in, the only time it
  can be cut, costs a ship a hit, and it takes about six of them to cut (≈ 60 HP each with the
  plan's fit). With two slams to a ship that could not be done: an **arm-first** pilot (the
  autopilot's `ARMS` play: it cuts the arm guarding the most ships, baits each slam to the lane
  where it costs least by the Kraken's own rules, waits beside a telegraphed lane and cuts the arm
  awash) kept all three on **0 of 5** seeds, as the first pilot did. The smallest lever is the
  ships' HP: a cargo ship now takes **4 slams at medium** (easy 5, hard 2 as before), the spare
  slams of the left half's two ships (six) covering the cut. Measured (KrakenTest's fixture /
  the whole level, five seeds each):

  | Pilot | Easy: all three afloat | Medium: all three afloat, fight | Hard |
  |---|---|---|---|
  | Eyes first (before, 2 slams) | 0 of 5 (8 of 15 ships) | 0 of 5 (5 of 15 ships), 59.0 s | 0 of 15 ships |
  | Eyes first (now) | 0 / 0 of 5 | 0 / 0 of 5 (10 of 15 ships), 62.5 s | 0 of 15 ships |
  | Arm-first (now) | 5 / 5 of 5 | **5 / 3 of 5** (15 / 12 of 15 ships), 58.6 / 55.5 s | 0 of 15 ships |

  The level's runs are harder than the fixture's (Rook worn down by the approach, a retry
  restarting the fight). The typical haul keeps the secondary at the economy's **50 %**: a player
  who plays the arms keeps the convoy most of the time, one who plays the eyes loses it, and the
  typical player is between them. Hard stays harsh: two slams sink a ship and no pilot of ours
  keeps one.

## Music & ambience

The Act 2 B theme **"Firestorm"** (track 7 in the [track list](../../../audio/music/README.md#track-list),
final with its base stem since Level 09): the ocean ambience alone in section 1, the **base stem
from section 2** and the **full mix from section 4** (the Tiamat approach), the **mini-boss sting**
(track 21) at the arena halt, when the boss bar appears, then the theme with the full mix
(default). Ambience: the [ocean ambience](../../../audio/sfx/README.md#ambience-per-setting)
(`earth-ocean`) from t=0 throughout. The frigate's flak bursts play a distant flak sound, quiet,
while it is on screen. New sounds for the Kraken, the ships and the frigate are listed in
[sfx](../../../audio/sfx/README.md#enemies); the torpedo's launch and the water explosion are the
chosen round-03 files.

## Credit budget

Budget(11) = 700 × 1.07¹⁰ ≈ **1,377**, the typical haul's target
([economy](../../../systems/economy/README.md#per-level-budget)), not a perfect collection. The
level's **`bounty_scale` of 0.6** puts the typical haul on it (1,414, +2.7 % with the Kraken's 216;
`TypicalHaulTest`,
±5 %); the first estimate was 0.6–0.8 (few air units, cheap naval ones), and the popcorn the pacing
rule needs is kept cheap (Mote Swarms, Skitters) so the scale stays at its lower end.
**The data keeps Act 1 terms** (default): the stat-block bounties (Driftjelly 10, Reef Spitter 14,
Skitter 5, Needler 12, Stinger 15), the sunken pod's crate 100, the secondary 100, the container
salvage 10; the code pays each × the act factor 1.6 (bounties also × the scale). The
**Kraken's bounty** (**216** in Act 1 terms: head 144, arms 36 each, arms : head = 1 : 4 per arm;
raised from 186 on 2026-10-09) pays **208** at the scale of 0.6, **15 %** of the budget, inside the
mid-boss band of 10–20 % (`BalanceTest`, `tools/balance.py`); the boss counts at 100 % in the typical
haul. Submerged jellies count at the ground rate (80 %) though a player without a torpedo kills them
only surfaced: the autopilot with the plan's fit kills **12–18 of the 28** before the arena at
medium (about half), ≈ 80 credits (6 % of the budget) less than the model counts; its whole haul is
above the budget anyway (1,519–1,611 at medium, it kills far more air units than the model's 60 %),
so the scale is not raised for it (for review in round 33). The draft's 1,967 was the old
perfect-run curve with Act-2-scaled amounts (jelly 16, container 16, crate and secondary 160, the
Kraken "300 absolute").

<!-- data: credit-budget -->
| Source | Perfect run | Typical haul |
|---|---|---|
| Kills: Skitter 110 × 5 + Mote Swarm 96 × 2 + Driftjelly 28 × 10 + Needler 19 × 12 + Stinger 4 × 15 | 1,306 | 840 |
| Ground targets: Reef Spitter 12 × 14 + floating containers 8 × small salvage 10 | 284 | 207 |
| Mid-boss: Harbour Kraken (head 144 + left arm 36 + right arm 36) | 208 | 208 |
| Secret: sunken pod (hidden crate, 7% of budget) | 160 | 80 |
| Secondary: convoy afloat (all 3 cargo ships afloat at the Harbour Kraken's death) | 160 | 80 |
| **Total** (bounty scale 0.6) | **2,118** | **1,414** |
| Budget(n) = the typical haul's target; typical +3 %, perfect 1.54 × budget | | 1,377 |
<!-- /data -->

**Balance plan** (default): the plan's L11 visit buys **no** Torpedo Pod (it is optional); it
refits **Rook's Mortar** for the rafts and the surfaced Kraken (`fit: [[escort, mortar]]`) and keeps
the front upgrade ([balance plan](../../../player/balance-plan.yaml)). `BalanceTest` measures the
Kraken's duration with its exposure windows (E6 = a: 57.4 s at the plan's DPS) and its share after
the scale (15 %). On hard the plan's Mortar was refused for credits at the Level 09 visit (accepted
there), so the refit is skipped and Rook keeps his Autocannon; `BalanceTest` accepts that refit too.

## Difficulty notes

- **Easy**: cargo ships survive four slams (sink on the fifth; the convoy's `easy: {hp: 5}`); the t=50
  and t=100 jelly fields have 6 jellies each; the other waves by the formation lever.
- **Hard**: cargo ships sink on the second slam (the convoy's `hard: {hp: 2}`); an extra Reef Spitter
  nest of 3 at t≈140 (x 50, 430, 46); the sunken pod needs six
  torpedo hits; the
  Kraken's hard hooks come from its spec (lane telegraph 0.8 s, phase 3 slams three lanes); the
  jellies' 12-bullet rings and 110 px trigger (their spec).

## Concept art

The production backdrop's review is below ([prompts](concept/prompts.md)). The look comes from the chosen ocean scene in
[art direction](../../../art-direction/README.md):
[scene-ocean-r10-a.png](../../../art-direction/concept/scene-ocean-r10-a.png) and its
[scroll loop](../../../art-direction/concept/scene-ocean-r10-a.gif). Part E (user decision E9 = a)
takes the backdrop (six sections, the reef line, the burning freighter, Platform Tiamat), the
briefing images and the props straight to production for concept round 33; a/b only for the ship
pair and for the lane telegraph and slam look. The round's captures of this level land in this
`concept/` directory. Closed 2026-10-09 (user): everything approved as final and accepted.

| File | What | Status |
|---|---|---|
| [l11-props-final-r33-a.png](../../../art-direction/concept/l11-props-final-r33-a.png), [l11-props-final-r33-a.gif](../../../art-direction/concept/l11-props-final-r33-a.gif) | Final props (`tools/art/l11_props.py`, kept in art-direction's concept directory): `floating-container` (with `-collar`, `-break`), `sunken-pod` (with `-rise`) and the backdrop pieces `burning-freighter`, `reef-growth-a/b/c`, `reef-root-a`. `backdrop_l11.py` imports `l11_props.pieces()` and writes the pieces into the backdrop | chosen |
| [concept/backdrop-final-r33-a.png](concept/backdrop-final-r33-a.png), [.gif](concept/backdrop-final-r33-a.gif) | Production backdrop straight from scene-ocean-r10-a (`tools/art/backdrop_l11.py`, notes in [concept/prompts.md](concept/prompts.md#backdrop-final-r33-a); its block for the level data is [concept/backdrop-proposal.yaml](concept/backdrop-proposal.yaml)): the sea in two layers (the swell on `deep` at 0.85 under the chop on `ground`), sea mist, spawn slicks, seven reef lines with channels for the convoy, the oil slick, flotsam, the burning freighter and its smoke, Platform Tiamat halting with its centre at (240, 110); composites with the convoy and, at the halt, the Kraken's grip and head; the GIF the approach, the halt and the scroll resuming | chosen |
| [concept/level-11-capture-final-r33-a.png](concept/level-11-capture-final-r33-a.png), [.mp4](concept/level-11-capture-final-r33-a.mp4) | Capture of the game (round 33, notes in [concept/prompts.md](concept/prompts.md#level-11-capture-final-r33-a)): the whole level at medium with the plan's L11 fit and Rook's Mortar, the arms first (left arm cut after four slams), all three hulls afloat; the video with the game's sound | chosen |
| [concept/kraken-capture-final-r33-a.png](concept/kraken-capture-final-r33-a.png), [.mp4](concept/kraken-capture-final-r33-a.mp4) | Capture of the Harbour Kraken's arena (round 33, [notes](concept/prompts.md#kraken-capture-final-r33-a)): Tiamat scrolling in, telegraphs, slams, awash arms, the head's surfacing and fans, the left arm severed, the death and the convoy holding clear of Tiamat; with sound. Retaken after round 33's feedback (2026-10-09): Platform Tiamat raised to y 78, the head surfacing and sinking in open water off the deck's south edge, flesh hits instead of water ripples, slamming arms bending and whipping, only the part hit flashing lightly; with the t≈55 foreshadowing (the torpedo crops are in the [torpedo capture](concept/prompts.md#torpedo-capture-r33-a)) | chosen |
| [concept/torpedo-capture-r33-a.png](concept/torpedo-capture-r33-a.png), [.mp4](concept/torpedo-capture-r33-a.mp4) | Capture with `left=torpedo-pod:3` (round 33, [notes](concept/prompts.md#torpedo-capture-r33-a)): hits under the water, the sunken pod freed and its crate caught (secret 1 / 1), the arm cut early; `NO WATER` cannot show on L11 | chosen |
| [concept/level-11-readability-r33-a.png](concept/level-11-readability-r33-a.png) | Readability crops at 1:1 and 2× (round 33, [notes](concept/prompts.md#level-11-readability-r33-a)): jellies surfaced and submerged, a raft, ships with wake and collar, telegraph, slam, awash arm, arms under the mantle, the head with eyes open and under fire, the CONVOY tracker, the debrief's HULLS AFLOAT row | chosen |

## Implementation

- [x] Level data `data.yaml` with every block below and this README's marked tables rendered from
      it; `PartELevelKeysTest`, `Level11Test` (M5 part E, step E3a)
- [x] `water: true` (E1 = a) read by the torpedo, the HUD and the game's water looks (M5 part E,
      steps E2a and E3c)
- [x] Scroll timeline, sections, atmosphere and layer content as in *Layout*; the arena section
      with `speed: 0` halting the scroll with Tiamat at the top (M5 part E, steps E2c, E3a, E3b)
- [x] Production backdrop: six sections, reef line, burning freighter, Platform Tiamat placed at
      the halt, the motion budget (M5 part E, step E3b; approved as final in round 33)
- [x] The convoy block: four hulls at their stations, the glide to lanes 1, 2, 4 and the frigate's
      exit at the halt, the return in section 6, on the real steps; slams the only damage, four hits
      (easy five, hard two), the sinking; never fails the mission; kept by the boss checkpoint (M5
      part E, step E2b; the hits retuned 2026-10-09)
- [x] After the Kraken the ships hold clear of Platform Tiamat (`hold_clear`, Mbeki's `hold`) until
      its deck has passed, then glide back (`Level11Test`) (M5 part E, 2026-10-09)
- [x] The convoy bonus measured: an arm-first pilot keeps all three most of the time at medium,
      the fight ≈ 60 s (`KrakenTest`, `Level11Test`) (M5 part E, 2026-10-09)
- [x] Secondary `afloat` (E8 = a): met at the Kraken's death with all three afloat, failed at the
      first sinking, 100 in Act 1 terms; the `CONVOY` tracker with three pips and the debrief row
      `HULLS AFLOAT n / 3` (M5 part E, steps E2b and E3c)
- [x] Wave script: the set pieces with the popcorn, `field` jelly waves; at least 50 enemies a
      minute at medium up to the arena (`DensityTest`); no empty screen over 3 s of real time on
      every difficulty (`PacingTest`) (M5 part E, step E3a)
- [x] Ground targets: the raft nests (hard's extra nest) on the flanks, off the hulls
      (`Level11Test`, 2026-10-09), the eight floating containers (salvage, the last one's armour
      patch: a destructible `group`, step E3a), kills on water sinking (M5 part E, steps E2b and
      E3a; the sinking drawn in step E3c)
- [x] The sunken pod: a `sub` trigger of 3 torpedo hits (hard 5: a trigger's `hard: {hits}`, step
      E3a) that torpedoes seek (2026-10-09), freed at once by a Smart Bomb, floating up as a 100
      crate (Act 1 terms) (M5 part E, steps E2a and E3a; the scanner's glint through the water is
      drawn in step E3c)
- [x] Radio voiced, every timed line at most 1 s late (`RadioTimelineTest`, with Rook and without
      him); the events `first-telegraph`, `boss-part-destroyed` and `ally-hit`; ship names as
      `{ally}`; Rook's lines `requires: escort`; Atlas Control cast in round 33 (E10 = a) and voiced
      (M5 part E, steps E2b, E3a, E1a and E4; Atlas Control cast in round 33, b, MaryAnn, and her
      ten takes rendered at the close; `RadioTimelineTest` covers the level)
- [x] Threat profile (`anti-sub` optional, not required), Varga's four intel lines and Rook's
      teaser fitting the intel panel (`IntelPanelLayoutTest`); the Driftjelly's and Reef Spitter's
      intel portraits and the Kraken's silhouette (M5 part E, steps E3a and E3e)
- [x] Briefing: Okafor's and Varga's pages, each one screen (`BriefingLayoutTest`), with their
      images (M5 part E, steps E3a and E3e)
- [x] Typical haul at medium within ±5 % of budget(11) = 1,377 with the level's `bounty_scale`
      (`TypicalHaulTest`); the Kraken's paid share 10–20 % (`BalanceTest`) (M5 part E, steps E2c
      and E3a)
- [x] Balance plan: the L11 visit refits Rook's Mortar, no torpedo; the autopilot flies the level
      to its end with Rook on every difficulty, the Kraken in about 60 s at medium (M5 part E,
      steps E2c and E3a)
- [x] Music: "Firestorm"'s base stem from section 2, the full mix from section 4, the sting at the
      halt; the ocean ambience from t=0; the frigate's flak (M5 part E, steps E3a and E3d)
- [x] Easy/hard variations as in *Difficulty notes* (M5 part E, step E3a)
- [x] Captures for concept round 33: the level, the Kraken fight, a torpedo run (M5 part E, step E5;
      the Kraken's retaken after the user's feedback, all accepted)

## Open questions

- None (concept round 33 closed 2026-10-09).

## Decisions

- 2026-10-01: Promoted from the Act 2 roster to a draft level document.
- 2026-10-01: Primary `reach-end`; the convoy ships only matter for the secondary objective, so
  a sunk ship loses its bonus but never fails the mission.
- 2026-10-01: Open question resolved: the Kraken's lane-choice and ship-damage rules moved into the Harbour Kraken spec; the arena notes link to it.
- 2026-10-01: The cargo ship and frigate specs moved to [allies](../../../allies/README.md).
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-08: M5 part E (user decisions of 2026-10-08, all as recommended): **E1 = a** a level flag
  `water: true` (the whole play field is water; Levels 11–13); rejected: b (a section flag: more
  data and a HUD that switches mid-level; for a later coastal level) and c (a water mask from the
  backdrop: far more work). **E2 = a** only `anti-sub` (and the Smart Bomb) reaches the `sub` layer
  ([enemies](../../../enemies/README.md#layer-rules)). **E3 = b** submerged jellies fire their ring
  too, their pulse showing through the water; rejected: a (only surfaced ones fire: tamer fields,
  the torpedo only for credits) and c (a half ring when submerged: a second attack profile).
  **E4 = c** the hybrid water drawing ([art direction](../../../art-direction/README.md)). **E5 = a**
  each slam arm owns a half of the lanes; **E6 = a** the Kraken's HP tuned to a ≈ 60 s fight;
  **E7 = a** its two slam arms as runtime chains ([Harbour Kraken](../../../enemies/bosses/harbour-kraken/README.md)).
  **E8 = a** the secondary all or nothing at the Kraken's death; rejected: b (pay per hull afloat:
  the haul and the tracker change) and c (both: two payouts). **E9 = a** art straight to production,
  a/b only for the ship pair and the lane telegraph and slam look; rejected: b (also the Kraken's
  surfacing and the platform: the largest render twice) and c (everything straight: the ships an
  unseen design). **E10 = a** Atlas Control auditioned a/b in round 33; rejected: b (reusing an Act 1
  voice, against round 19's "every role its own voice") and c (cast without audition).
- 2026-10-08: M5 part E stated defaults (accepted with the decisions): the data in Act 1 terms
  (crate 100, secondary 100, container salvage 10, Reef Spitter 14, Driftjelly 10) with a
  `bounty_scale` fitted by `TypicalHaulTest` (estimate 0.6–0.8); the Kraken's paid bounty ≈ 15 % of
  1,377 with arms : head 1 : 4; no act HP factor (the returning units are `tiny`/`small`); the waves
  densified with popcorn to at least 50 a minute and the 3 s pacing rule, no rear waves, the two
  introductions as quiet windows of at most 3 s; the new formation `field`; submerged units
  scrolling with the sea; each jelly's own seeded swap timer, a field starting about half
  submerged; torpedoes hitting `sub` and every `ground` unit over water at full damage; the torpedo
  pod idle and reading `NO WATER` without water; the Bomb Rack, the mortars and the Airstrike over
  water unchanged (they hit surfaced units only) with a water splash; the Smart Bomb hitting `sub`;
  Rook skipping `sub`, keeping out of telegraphed and slamming lanes, hit by slams; the plan's L11
  visit refitting Rook's Mortar, no torpedo; lane choice "nearest ship" by the player's x (ties to
  the lower lane), a slam hitting the player and a ship in one lane both; the convoy at stations in
  the lower half, nothing hurting it before the arena, the glide to lanes 1, 2, 4 and the frigate
  dropping back; the frigate's flak presentation only; the arena's jelly field released at the first
  surfacing (the level clock halts at the arena); the sting at the arena halt; "Firestorm"'s base
  stem from section 2, the full mix from section 4, the ocean ambience throughout; the `CONVOY`
  tracker with three pips and `HULLS AFLOAT n / 3`; the sunken pod counting hits (3, hard 5) and
  freed by a Smart Bomb; the boss checkpoint keeping the ships; ship names as `{ally}`; flyers'
  shadows on the sea; two one-screen briefing pages, Rook's teaser, Varga's four lines; the radio
  retimed to the 1 s rule with the events `first-telegraph`, `boss-part-destroyed`, `ally-hit`,
  `ally-lost`, `boss-destroyed` and `secondary-objective`, no mission-failed line; `submerged
  ambush` retagged to Act 4. The draft's contradictions are fixed: the budget (1,967 on the old
  curve → budget(11) ≈ 1,377), its Act-2-scaled values, the Kraken's "300 absolute", the density
  (≈ 27 a minute), the radio clashes, the single Varga line, the unattributed teaser, the sting "on
  entry" against "when the Kraken surfaces" (now: at the halt), "swarm (field)" (now `field`). Our
  readings, for review in round 33: the station and lane first values; the arena section with
  `speed: 0` and the anchored boss; the briefing, intel, teaser and radio texts; the second
  level-end line. `design` goes to `review` for the new texts; `implementation` is `in-progress`.
- 2026-10-08: M5 part E, step E3a (the level data): the waves keep the set pieces and fill the gaps
  with cheap Act 2 popcorn (Skitter streams and snakes, Mote Swarms of 8, a few Needler lines), 254
  enemies before the arena at medium (≈ 98 a minute; the pacing rule needs about that many, as in
  Levels 09 and 10), and `bounty_scale` **0.6** puts the typical haul on budget(11) (1,385 of
  1,377); the Kraken's paid share is then 13 % (its bounty was written for ≈ 0.69). The second raft
  nest moved from t≈92 to t≈101 (with the t=100 field), so no raft draws the torpedoes off the
  sunken pod; the pod enters at t≈93.8 on the backdrop's reef root (x 296). The floating containers
  are a destructible `group` of their own (new: the pickup on the group drops from its last
  destructible once all are destroyed; the objectives never count it) and the pod's hard hits a
  trigger's `hard: {hits: 6}` (new). The convoy's first values stand (stations, lanes 1, 2 and 4 at
  y 450, a 3 s glide, easy 3 HP). The radio keeps step E0's times (as text every timed line on time).
  Settled for review in round 33: one Torpedo Pod at L1 lands only 3 of the pod's 4 hits in its
  pass; the autopilot (no torpedo) kills about half the jellies, against the model's 80 %; it never
  meets the convoy secondary (5 of 15 ships afloat at medium on five seeds); after the halt the ships
  glide straight back to their stations while Platform Tiamat scrolls down through Mbeki's (E3b's
  "hold clear" suggestion is not built). The intel panel (`IntelPanelLayoutTest`) holds two lines
  of Varga's quote beside this level's rows (hazard, objective, boss, five portraits): her L2 and L3
  lines are shortened ("New: Driftjellies and Reef Spitters. And something huge.", "Anti-ground for
  rafts. Torpedoes for the sunken cache.") and the recommended traits are `spread` and
  `anti-ground` only (a third does not fit the row; `anti-sub` stays optional in the notes).
- 2026-10-09: The convoy bonus, measured then tuned (user: "measure, then tune"). The autopilot
  learned an **arm-first** play (`ARMS`: the arm guarding the most ships first, each slam baited to
  where it costs least by the Kraken's lane rules, the arm cut while it lies awash; the old play
  stays as `EYES` for the earlier measurements). It showed that no pilot could keep the ships with
  two slams each: every left-half slam strikes Halvorsen's or Mbeki's lane and the left arm takes
  about six awash slams to cut. Lever: **the cargo ships' HP** (the slams they take), 2 → **4** at
  medium, easy 3 → 5, hard kept at 2 (`hard: {hp: 2}`); rejected: the lane rule (how often a slam
  goes for a ship: the left half has no free lane, so a left-arm slam costs a hit whatever the rule),
  the slam cadence (a longer awash breaks the 4 s volleys) and 3 HP (arm-first kept all three on
  0 of 5 seeds). Result at medium: arm-first all three afloat on 5 of 5 fixture seeds and 3 of 5
  whole-level runs, the fight 58.6 / 55.5 s; eyes first on none. The typical haul keeps the
  secondary at 50 %. The ships **hold clear of Platform Tiamat** after the Kraken (`hold_clear: 520`
  px, Mbeki's `hold` off the bottom edge), the rafts moved to the **flanks** (an 84 px raft does not
  fit a 54 px gap between hulls), and the Kraken's bounty is **216** (paid 208, 15 % of the budget).
  Torpedoes now seek the sunken pod; a lined-up L1 pod frees it when the pilot fires from a second
  before the pod enters (out of step it lands 3 of 4: the pass is ≈ 4 s at 0.8 torpedoes a second,
  which seeking does not lengthen).
- 2026-10-09: Round 33 (user): the Kraken's head surfaces off the deck's south edge, not through
  the platform, so the arena halt **raises Platform Tiamat 32 px** (option b): its centre at
  (240, **78**) at the halt, the deck at y 2–154, the lanes from y 154 (`backdrop_l11.py`
  `PLATFORM_AT`, the piece placed at t 162.863, ground 22,930.83 px; the Kraken's `anchored: {y: 78}`)
  and the convoy's `hold_clear` **552** (the deck's top edge 538 px above the bottom edge at the
  halt). The **sunken pod takes 3 torpedo hits (hard 5)** instead of 4 (6): one Torpedo Pod at L1
  lined up frees it whether the pilot fires in step or out of step, and 24 px off the line its
  seeking torpedoes free it too; on hard it takes a pod on each wing (`Level11Test`). The fight's
  numbers are in the [Kraken's Decisions](../../../enemies/bosses/harbour-kraken/README.md#decisions).
- 2026-10-09: Concept round 33 closed (user): the backdrop, the props, the briefing images, the
  intel pictures and the Driftjelly's, Reef Spitter's and Harbour Kraken's art approved as final, so
  `art: final`; the convoy ship pair **a** "Atlantic line" (b "Reactor run" rejected); the lane
  telegraph and slam **a** "edge dashes + spray sheets" (b rejected); Atlas Control's voice **b**
  (MaryAnn; her ten takes rendered at the close); the eight sounds accepted, the churn, death,
  sinking and flak after a rework (no bubbles; the flak raised twice to −16 dB), produced into
  `assets/sfx/`. After the user's feedback the Kraken's head surfaces and sinks off the deck's south
  edge with Platform Tiamat raised (centre y 78, option b), with flesh hits, whipping arms and a
  part-only flash, and the sunken pod takes 3 torpedo hits (hard 5, build choice n); the retaken
  Kraken capture accepted. The captures, the 15 voiced lines, the texts, the part E numbers (hard
  kept as built: "hard is supposed to be hard"), the decisions and every build choice (a)–(y)
  accepted; the texts approved, so `design: approved`; every Implementation item is ticked, so the
  implementation is `done`.
