---
title: Level 11 – Atlantic Convoy
design: draft
implementation: not-started
art: chosen
depends-on: [../../../enemies/bosses/harbour-kraken, ../../../enemies/naval/driftjelly, ../../../enemies/naval/reef-spitter]
updated: 2026-10-01
---

# Level 11 – Atlantic Convoy

## Summary

The first sea level and the Act 2 mid-boss. Convoy **Atlas-Seven** carries reactor parts for
the Arctic relays across a grey Atlantic that the Vrell have seeded with drifting
[Driftjellies](../../../enemies/naval/driftjelly/README.md) and reef-grown
[Reef Spitter](../../../enemies/naval/reef-spitter/README.md) gun rafts. The level introduces the
naval surface layer and shows the `sub` layer for the first time as shadows under the waves. At
the overrun fusion platform Tiamat the scroll halts for the mid-boss, the
[Harbour Kraken](../../../enemies/bosses/harbour-kraken/README.md). About 2 minutes 40 seconds of
scroll plus the mid-boss and a short exit.

## Briefing

> **Commander Okafor:** "Lancer, Aegis Actual. The Arctic defence relays are running on reserve
> power. Convoy Atlas-Seven is carrying their reactor parts across the Atlantic: three cargo hulls
> and a frigate. The Vrell have seeded the sea lanes. Expect drifting organisms on and under the
> surface, and gun rafts grown out of the reefs. Something large has been tracked near Platform
> Tiamat, the fusion rig on the convoy route. If it shows itself, kill it before it reaches the
> ships. Our guns can't touch anything below the waves. Torpedo pods can, if you've bought them.
> Get the convoy through to open water. Aegis Actual out."

*Hangar teaser* (shop screen before L11): "Atlantic convoy. Something big under the waves, and
torpedo pods are now in the shop."

*Varga's intel line*: "On the sea, surface targets count as ground, Lancer: anti-ground doubles
your damage. Below the surface only anti-sub reaches, and spread fire handles the jellies."

## Threat profile

| Field | Value |
|---|---|
| Dominant layers | `ground` (naval surface: rafts, surfaced jellies, the Kraken's arms), `sub` (submerged jellies, the Kraken, a sunken cache), `air` |
| Attack directions | front 90% · sides 10% · rear 0% |
| Density | 3 |
| Recommended traits | `spread`, `anti-ground`; `anti-sub` optional (submerged targets, the Kraken's head in phase 1) |
| Hazards | Driftjelly fields (area denial on and under the surface) |
| Boss / mid-boss | Mid-boss [Harbour Kraken](../../../enemies/bosses/harbour-kraken/README.md) |
| Sensor-suite detail | **none**: open ocean, ground + sub + air, front. **L1**: front 90 / sides 10, density 3, hazard "drifting organisms". **L2**: Driftjelly (new), Reef Spitter (new), Needler, Stinger, Skitter portraits; mid-boss silhouette "unknown, large, submerged". **L3**: `spread` and `anti-ground` highlighted (`anti-sub` marked optional); the timeline strip marks the mid-boss at ≈160 s; 1 secret. |

## Objective

- **Primary** `reach-end`: reach open water with the convoy. The scroll stays halted at Tiamat
  until the Kraken is destroyed. The convoy ships don't count for the primary: losing them
  never fails the mission.
- **Secondary**: no convoy ship sunk during the Kraken fight. Pays +160 credits.

### The convoy

Level-specific friendly units (first-draft numbers): three cargo ships (**Halvorsen**,
**Mbeki**, **Saint-Laurent**) and the escort frigate **CDFS Ruyter** on the `ground` layer
(naval surface). They steam at the scroll speed, so they hold station in the lower half of the
screen while the sea streams past, as in the chosen ocean scene. Before the Kraken they take no
damage: Reef Spitters and Driftjellies fire at the player, and the frigate's flak bursts are a
visual cue only. In the Kraken arena each cargo ship survives **one** slam and sinks on the
second. The frigate stays out of the arena.

## Layout

Ground scroll speed **140 px/s** (the chosen ocean scene's slower swell). Total ≈ 235 s:
160 s of scroll (22,400 px), the halted mid-boss (target 45–75 s; the timeline assumes 60 s),
then 15 s of exit (2,100 px). The Water rules from the [art direction](../../../art-direction/README.md)
apply to everything that meets the surface: foam collars, ripple trains, submerged parts visible
and connected, nothing pops in. Motion budget: the sea and the wakes are the two strong
elements; mist, smoke and whitecaps stay faint.

| Section | t (s) | Scroll (px) | Speed (px/s) | Atmosphere | Layers and content | Purpose |
|---|---|---|---|---|---|---|
| 1. Convoy Muster | 0–20 | 0–2,800 | 140 | `light` (sea mist) | `deep`: slate swell with a slow colour field. `ground`: the convoy in formation, wakes and prop-wash. `sub`: nothing yet. `low-air`: sea-mist banks (~15%). `high-air`: thin wisps. | Meet the convoy; no enemies. |
| 2. Jelly Fields | 20–65 | 2,800–9,100 | 140 | `light` | `ground`: Driftjelly fields drifting in the current. `sub`: submerged jellies as tinted shapes; at t≈55 a huge dark shape (≈ 400 px) slides under the convoy and fades into the deep (the Kraken, foreshadowing). | Driftjellies introduced alone first; the `sub` layer seen as shadows. |
| 3. Reef Line | 65–110 | 9,100–15,400 | 140 | `medium` (mist banks between the reefs) | `ground`: violet-veined Vrell reef growths breaking the surface in lines, with foam collars; Reef Spitter rafts bobbing on kelp. `sub`: reef roots fading into the deep, a CDF supply pod snagged on one (the secret). `low-air`: mist banks (~22%). | Reef Spitters introduced as a lone nest of 3, then combined with a jelly field. |
| 4. Tiamat Approach | 110–160 | 15,400–22,400 | 140 | `light` | `ground`: debris and floating cargo containers from a burning freighter (smoke at half particle speed, per the motion budget); the overrun fusion platform Tiamat appears at the top at the end, gripped by armoured arms. `sub`: the Kraken's mantle shadow under the platform. | Mixed waves; the last Reef Spitter nest; the flotsam pickups. |
| 5. Harbour Kraken | 160–220 (halted) | 22,400 | 0 | `clear` | `ground`: platform Tiamat at the top of the play field, four lanes of 120 px below it, the three cargo ships holding station in lanes 1, 2 and 4 in the lower band. `sub`: the Kraken's body and idle arms visible under the surface. | The mid-boss. |
| 6. Open Water | 220–235 | 22,400–24,500 | 140 | `clear` → `light` | `ground`: the convoy steaming north past the abandoned platform. `deep`: the swell under a lighter overcast. | Release; level end. |

## Waves

Enemy definitions are linked per row; formation names come from the
[formation vocabulary](../../../enemies/README.md#formation-vocabulary). Returning units use the
act HP factor from the [balancing basis](../../../enemies/README.md#balancing-basis); HP values
live in the enemy specs only. Jelly fields scroll in with the sea; each jelly swaps between
surfaced and submerged every 6–10 s (see its spec).

| t (s) | Section | Formation | Enemies | Count | Enter from | Notes |
|---|---|---|---|---|---|---|
| 22 | 2 | swarm (field) | [Driftjelly](../../../enemies/naval/driftjelly/README.md) | 6 | front | **New.** Nothing else on screen; the ring only fires when the player is within 96 px |
| 32 | 2 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 5 | front | |
| 40 | 2 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 8 | front | Weaves above the jelly field |
| 50 | 2 | swarm (field) | Driftjelly | 8 | front | Denser field |
| 84 | 3 | V-wing | [Stinger](../../../enemies/air/stinger/README.md) | 4 | front | The leader drops an overdrive |
| 100 | 3 | swarm (field) | Driftjelly | 8 | front | Between the reef lines, with the second raft nest |
| 118 | 4 | pincer | Needler | 4 | sides | |
| 140 | 4 | swarm (field) | Driftjelly | 6 | front | Among the flotsam |
| 146 | 4 | stream | Skitter | 10 | front (alternating edges) | |
| 165 | 5 | swarm (field) | Driftjelly | 6 | outer lanes of the arena | Released when the Kraken first surfaces (the spec's "Driftjelly fields around the arena") |

Totals: Driftjelly 34 · Needler 9 · Skitter 18 · Stinger 4.

## Ground targets

| Section | Target | Effect |
|---|---|---|
| 3 (t≈70) | [Reef Spitter](../../../enemies/naval/reef-spitter/README.md) turret nest ×3 | **New.** A lone nest, no other enemies; the raft sinks when the gun dies |
| 3 (t≈92) | Reef Spitter turret nest ×4 | With the t=100 jelly field |
| 4 (t≈128) | Reef Spitter turret nest ×5 | Beside the burning freighter |
| 4 (t≈112–135) | Floating cargo containers ×8 (`ground`, 3 HP each) | Each drops a 16-credit salvage pickup; the last one drops an armour patch |
| 3 (t≈96) | Snagged CDF supply pod (`sub`) | Secret, see below |

## Hazards

- **Driftjelly fields**: the jellies' proximity rings make flying low over a field costly;
  submerged jellies can only be hit with `anti-sub` and surface again within 6–10 s.
- No terrain collision; reef growths are ground scenery.

## Secrets and pickups

- **Sunken supply pod** (t≈96): a CDF pod snagged on a reef root on the `sub` layer. Only
  `anti-sub` weapons (the [Torpedo Pod](../../../player/weapons/torpedo-pod/README.md)) or a
  Smart Bomb reach it; four torpedo hits free it and it floats up as a crate worth **160**
  credits. A reward for buying anti-sub early.
- **Overdrive**: dropped by the t=84 Stinger leader.
- **Armour patch**: from the last floating container; a shield cell drops when the Kraken goes
  from phase 2 to phase 3.

## Radio chatter

| Trigger | Speaker | Line |
|---|---|---|
| t=1 | CDF officer (Atlas Control) | "Aegis flight, Atlas-Seven. Three hulls of reactor parts for the Arctic grid. We'd like to arrive with all three." |
| t=3 | Rook | "Long way to swim, Atlas. We'll keep you dry." |
| t=20 | Varga | "Those jellies drift with the current. Don't hover over them: they pulse when you're close." |
| t=26 | Varga | "Some are under the surface. Our guns can't reach them there. Torpedoes can." |
| t=55 | Rook | "Lancer... did the sea just move under the convoy?" |
| t=56 | CDF officer (Atlas Control) | "Sonar has a contact. Big. Very big. It's gone deep again." |
| t=68 | Varga | "Gun rafts on the reef. Barnacle guns. Anti-ground rounds crack them twice as fast." |
| t=110 | CDF officer (Atlas Control) | "Platform Tiamat has been silent for six hours. Our route runs right past it." |
| t=150 | The Choir | "[the Choir sings]" |
| t=155 | Varga | "The contact is surfacing around the platform. Lancer, it's holding on to it." |
| t=158 | Rook | "Here comes the big one." |
| First lane telegraph | Okafor | "Watch the churning water. That's where the arms come down." |
| First slam arm severed | Rook | "Tell the cook we're having calamari." |
| A cargo ship is hit | CDF officer (Atlas Control) | "[Ship] is hit! Taking on water, but holding!" |
| A cargo ship sinks | CDF officer (Atlas Control) | "We've lost the [ship]. Crew in the water, and the frigate is picking them up." |
| Kraken destroyed | Varga | "It's letting go of the platform. It's sinking. Good." |
| Level end | Okafor | "Atlas-Seven is through. The Arctic grid gets its parts. Good work, Aegis." |
| Secondary objective met | CDF officer (Atlas Control) | "All three hulls afloat. Drinks are on Atlas, Aegis." |

## Boss / mid-boss

Mid-boss: [Harbour Kraken](../../../enemies/bosses/harbour-kraken/README.md). Its phases, HP and
attacks live in the spec. Arena notes for this level:

- The scroll halts with Tiamat at the top of the play field; the four slam lanes are 120 px wide
  and run the full height below the platform.
- The three cargo ships hold station in lanes 1, 2 and 4 in the lower band (y ≈ 400–500, on the
  `ground` layer under the player). A slam in a lane with a ship hits that ship; a second hit
  sinks it.
- **Lane choice** (level proposal): the Kraken alternates between the lane the player is in and
  the lane of the nearest surviving ship. Severed slam arms remove their lanes (spec), so cutting
  the arms during their awash window is how the player protects the convoy.
- Players with the optional Torpedo Pod can damage the submerged head in phase 1 (spec).
- The mini-boss sting plays on entry; the boss checkpoint follows the [retry](../../../systems/retry/README.md) rules.
- Bounty: **300** absolute (slam arms 50 each, head 200), not act-scaled again.

## Music & ambience

The Act 2 B theme "Firestorm" (track 7 in the [track list](../../../audio/music/README.md#track-list)),
starting at the section 2 transition. The mini-boss sting (track 21) plays when the Kraken
surfaces, then the level track continues with the intensity stem on. Ambience: the
[ocean ambience](../../../audio/sfx/README.md#ambience-per-setting) throughout, plus the frigate's
distant flak.

## Credit budget

The total matches budget(11) = **1,967** from the
[economy](../../../systems/economy/README.md#per-level-budget) curve. Bounties are the spec values ×
the act factor 1.6, rounded per kill: Skitter 8, Needler 19, Stinger 24, Driftjelly 16, Reef
Spitter 22; the mid-boss bounty is absolute.

| Source | Credits (medium) |
|---|---|
| Kills: Driftjelly 34 × 16 + Needler 9 × 19 + Skitter 18 × 8 + Stinger 4 × 24 | 955 |
| Ground targets: Reef Spitter 12 × 22 | 264 |
| Mid-boss: Harbour Kraken | 300 |
| Pickups: floating containers 8 × 16 | 128 |
| Secret: sunken supply pod | 160 |
| Secondary objective | 160 |
| **Total** | **1,967** |

## Difficulty notes

- **Easy**: cargo ships survive two slams (sink on the third); the t=50 and t=100 jelly fields
  have 6 jellies each.
- **Hard**: an extra Reef Spitter nest of 3 at t≈140; the sunken pod needs six torpedo hits;
  the Kraken's hard-mode hooks come from its spec.

## Concept art

No level-specific concept files. The look comes from the chosen ocean scene in
[art direction](../../../art-direction/README.md):
[scene-ocean-r10-a.png](../../../art-direction/concept/scene-ocean-r10-a.png) and its
[scroll loop](../../../art-direction/concept/scene-ocean-r10-a.gif). The convoy ships have no
sprite concept yet.

## Implementation

- [ ] Scroll timeline, sections, atmosphere intensity and parallax content per layer as in *Layout*.
- [ ] Water rules on every surface contact (jellies, rafts, reefs, the Kraken).
- [ ] Convoy ships holding station at scroll speed; damage only from Kraken slams; sinking.
- [ ] Halted scroll at Tiamat until the Kraken dies; lane choice as in the arena notes.
- [ ] `sub` layer shadows (jellies, the passing shape, the snagged pod) hittable only by `anti-sub`.
- [ ] Wave script matches the *Waves* table.
- [ ] Floating containers and the sunken-pod secret.
- [ ] Radio chatter cues with the ship-name variants.
- [ ] Secondary objective (no ship sunk) tracked and rewarded.
- [ ] Credit total at medium with perfect collection is 1,967 (± 5%).
- [ ] Easy/hard variations as in *Difficulty notes*.

## Open questions

- The Kraken's lane choice (the player's lane, alternating with the nearest ship's lane) is a
  level proposal. Should it move into the [Harbour Kraken](../../../enemies/bosses/harbour-kraken/README.md)
  spec?

## Decisions

- 2026-10-01: Promoted from the Act 2 roster to a draft level document.
- 2026-10-01: Primary `reach-end`; the convoy ships only matter for the secondary objective, so
  a sunk ship loses its bonus but never fails the mission.
