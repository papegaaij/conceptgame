---
title: Level 13 – Polar Relay
design: approved
implementation: not-started
art: chosen
depends-on: [../../../enemies/naval/skimmer, ../../../enemies/ground/scuttler]
updated: 2026-10-01
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

## Briefing

> **Commander Okafor:** "Lancer, Aegis Actual. Thanks to Atlas-Seven, Nansen Relay in the high
> Arctic is back at full power. It ties every gun, radar and satellite on the planet into one
> defence grid, and the Vrell have worked that out. A landing force is moving on it across the
> pack ice: fast boats in the leads between the floes, and walkers on the floes themselves. Hold
> position over the relay for three minutes while it synchronises the grid. They will come from
> every side, including behind you. If the relay falls, we lose the grid. Aegis Actual out."

*Hangar teaser* (shop screen before L13): "Defend the Arctic relay for three minutes. Attacks from
every edge: rear and side guns pay off."

*Varga's intel line*: "They'll come from all four edges, Lancer. Side and rear guns will earn their
keep, and spread fire for the boats."

## Threat profile

| Field | Value |
|---|---|
| Dominant layers | `ground` (Scuttlers on the floes, Polyp Mortars), `ground` naval surface (Skimmers), `air` |
| Attack directions | all edges (a `defend` level; it is excepted from the "≥ 60% front" rule and the hangar says so): front 44% · sides 39% · rear 17% (waves from all four edges are split evenly over them) |
| Density | 4 |
| Recommended traits | `spread`, `rear`, `side` |
| Hazards | Whiteout (edge visibility, final push); the relay must survive |
| Boss / mid-boss | none (finale: the whiteout push) |
| Sensor-suite detail | **none**: arctic, ground + air, all directions. **L1**: front 44 / sides 39 / rear 17, density 4, hazard "whiteout", defend objective. **L2**: Skimmer (new), Scuttler, Mantis, Needler, Stinger, Skitter, Polyp Mortar portraits. **L3**: `spread`, `rear` and `side` highlighted; the timeline strip marks the three defend phases and the whiteout; 1 secret. |

## Objective

- **Primary** `defend`: Nansen Relay must survive the 180 s synchronisation (t=40–220).
  - **Fail**: relay integrity reaches 0 → mission failed (see [retry](../../../systems/retry/README.md)).
- **Secondary**: relay integrity at or above 50% at the end. Pays +150 credits.

### Nansen Relay

The [Nansen Relay](../../../allies/README.md#nansen-relay) (footprint, integrity 600 at medium,
damage rules in the allies spec) stands on the `ground` layer at the centre of the play field
(x = 480, y ≈ 250 in screen coordinates). It is not repaired; the crew launch supply drones for
the player at t=160 and t=200 (see *Secrets and pickups*).

Relay targeting uses the
[target-the-objective hook](../../../enemies/README.md#target-the-objective-hook):

| Units | Mode |
|---|---|
| Skimmers (all waves) | `alternate` |
| Scuttlers (all waves) | `in-arc` |
| Stingers of the t=156 and t=210 waves | `always` |
| Polyp Mortar floe (t≈66) | `always` |

## Layout

Approach at **140 px/s**, then the scroll **halts** for the defend phase, then resumes for the
exit. Total ≈ 235 s: 40 s approach (5,600 px), 180 s halted, 15 s exit (2,100 px) = 7,700 px.
While halted, the floes keep drifting at 6 px/s and the open water in the leads moves gently.
Motion budget: the drifting floes with their waterline foam and, in phase 3, the whiteout banks
are the strong elements; the fog wisps stay faint.

| Section | t (s) | Scroll (px) | Speed (px/s) | Atmosphere | Layers and content | Purpose |
|---|---|---|---|---|---|---|
| 1. Pack Ice | 0–40 | 0–5,600 | 140 | `light` (thin fog) | `deep`: dark open water under the ice. `ground`: white-blue ice floes with open leads, teal submerged ice edges, waterline foam (Water rules), an abandoned research hut (the secret), the relay appearing at the top at t≈34. `low-air`: thin fog wisps. Low sun, long cool shadows. | Skimmer introduction during the approach; arrival at the relay. |
| 2. First Light (defend phase 1) | 40–100 | 5,600 (halted) | 0 | `light` | `ground`: Nansen Relay at the centre, floes drifting slowly round it, Skimmer wakes in the leads. | Learn to guard a point: boats from all edges. |
| 3. Floe Walkers (defend phase 2) | 100–160 | 5,600 (halted) | 0 | `light` → `medium` from t=140 (fog) | As above, with Scuttlers striding across the floes from the sides and fog thickening at the edges. | Walkers with armoured fronts: hit them from the side; side attackers. |
| 4. Whiteout (defend phase 3) | 160–220 | 5,600 (halted) | 0 | `medium`, `heavy` whiteout peak 176–188 (4 s ramps) | `low-air`: whiteout banks rolling in from the side edges, 25–40% coverage at the edges, the centre around the relay kept readable. `high-air`: snow streaks (≤ 40%). | The final push in poor visibility; edge warnings still show through the whiteout. |
| 5. Grid Online | 220–235 | 5,600–7,700 | 140 | `clear` | `ground`: the relay's lights coming on in sequence, then floes and open sea northward. | Release; level end. |

## Waves

Enemy definitions are linked per row; formation names come from the
[formation vocabulary](../../../enemies/README.md#formation-vocabulary). Returning units use the
act HP factor from the [balancing basis](../../../enemies/README.md#balancing-basis); HP values
live in the enemy specs only. Every side and rear wave gets the 1.5 s edge warning.

| t (s) | Section | Formation | Enemies | Count | Enter from | Notes |
|---|---|---|---|---|---|---|
| 20 | 1 | convoy | [Skimmer](../../../enemies/naval/skimmer/README.md) | 3 | front | **New.** Weaves along a lead between floes; nothing else on screen |
| 30 | 1 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 5 | front | |
| 56 | 2 | cross | Skimmer | 4 | all edges (1 each) | First all-edge wave; Rook: "pick a direction" |
| 84 | 2 | convoy | Skimmer | 4 | rear | Along the lead below the relay |
| 104 | 3 | pincer | [Scuttler](../../../enemies/ground/scuttler/README.md) | 4 | sides (2 + 2) | Walk across the floes toward the relay; claws armoured from the front |
| 118 | 3 | cross | Skimmer | 4 | all edges | |
| 124 | 3 | pincer | [Mantis](../../../enemies/air/mantis/README.md) | 2 | sides | Hold at the edges and sweep; the first one drops an overdrive |
| 132 | 3 | line abreast | Needler | 3 | front | |
| 140 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 12 | rear | |
| 146 | 3 | convoy | Skimmer | 4 | sides (left) | |
| 156 | 3 | V-wing | [Stinger](../../../enemies/air/stinger/README.md) | 4 | front | They dive at the **relay**, not the player |
| 164 | 4 | pincer | Scuttler | 4 | sides | |
| 176 | 4 | cross | Skimmer | 8 | all edges (2 each) | Into the whiteout |
| 184 | 4 | pincer | Mantis | 2 | sides | |
| 188 | 4 | circle | Needler | 6 | front | Orbits above the relay, then breaks toward the player |
| 196 | 4 | convoy | Scuttler | 3 | front | Down the main floe |
| 204 | 4 | cross | Skimmer | 4 | all edges | |
| 210 | 4 | column | Stinger | 4 | front | The last push: diving at the relay one after another |

Totals: Skimmer 31 · Scuttler 11 · Needler 14 · Mantis 4 · Stinger 8 · Skitter 12.

## Ground targets

| Section | Target | Effect |
|---|---|---|
| 2 (t≈66) | [Polyp Mortar](../../../enemies/ground/polyp-mortar/README.md) ×2 on a floe drifting in from the left at 10 px/s | They target the relay; credits |
| 1 (t≈12) | Abandoned research hut, roof hatch blinking | Secret, see below |

## Hazards

- **Whiteout** (176–188 peak): banks on `low-air` cover the side edges of the play field;
  enemy bullets and edge-warning arrows always draw above them.
- Floes are scenery on the `ground` layer: no collision.

## Secrets and pickups

- **Research hut cache** (t≈12): three hits on the blinking roof hatch release a CDF cache worth
  **180** credits.
- **Supply drones** (t=160 and t=200): the relay crew launch a drone carrying an armour patch;
  it flies a slow arc over the relay.
- **Overdrive**: dropped by the first Mantis of the t=124 pincer.

## Radio chatter

| Trigger | Speaker | Line |
|---|---|---|
| t=1 | Okafor | "Nansen Relay coordinates every gun on the planet. If it goes dark, so does the grid." |
| t=3 | Rook | "Cold enough for you, Lancer? My canopy's frosting on the inside." |
| t=18 | Varga | "Fast boats in the leads. They weave. Lead your shots, or spray." |
| t=36 | CDF officer (Nansen) | "Aegis, Nansen. Very glad to see you. They come at us from every direction out here." |
| t=40 | Okafor | "Lancer, you're holding station over the relay. Nothing touches it." |
| t=54 | Rook | "Contacts, all sides! Pick a direction, any direction." |
| t=82 | Rook | "Six o'clock, Lancer!" |
| t=102 | Varga | "Walkers on the floes. Their claws are armoured from the front. Hit them from the side." |
| t=122 | Rook | "Contacts on your flank!" |
| t=154 | Okafor | "Divers inbound. They're going for the relay, not you." |
| t=160 | CDF officer (Nansen) | "Supply drone away. Catch it, Aegis." |
| t=172 | Okafor | "Weather's turning. Whiteout coming in from the edges." |
| t=174 | The Choir | "[the Choir sings]" |
| Relay below 50% | CDF officer (Nansen) | "Integrity at half! We can't take much more!" |
| Relay below 25% | Okafor | "Lancer, the relay is failing. Everything else can wait." |
| t=218 | CDF officer (Nansen) | "Grid sync complete. Every battery on Earth just came back online." |
| t=220 | Okafor | "Well held, Aegis. Now we can see the whole board." |
| t=230 | Rook | "Next time they need a relay defended, I'm asking for one with palm trees." |
| Secondary objective met | CDF officer (Nansen) | "Integrity holding above half. You kept the lights on, Aegis." |
| Relay destroyed (fail) | Okafor | "Nansen is gone. Pull back, Lancer." |

## Boss / mid-boss

None. The whiteout push (t=176–220) is the finale.

## Music & ambience

The Act 2 A theme "Homefront" (track 6 in the [track list](../../../audio/music/README.md#track-list)),
starting when the scroll halts. The intensity stem comes in for defend phase 3, and the mission
complete jingle (track 23) plays on "Grid sync complete". Section 1 has ambience only: the
[arctic ambience](../../../audio/sfx/README.md#ambience-per-setting) (cold wind), which stays
under the music.

## Credit budget

The total is within 0.3% of budget(13) = **2,252** from the
[economy](../../../systems/economy/README.md#per-level-budget) curve. Bounties are the spec values ×
the act factor 1.6, rounded per kill: Skitter 8, Needler 19, Stinger 24, Mantis 48, Skimmer 22,
Scuttler 40, Polyp Mortar 24.

| Source | Credits (medium) |
|---|---|
| Kills: Skimmer 31 × 22 + Scuttler 11 × 40 + Needler 14 × 19 + Mantis 4 × 48 + Stinger 8 × 24 + Skitter 12 × 8 | 1,868 |
| Ground targets: Polyp Mortar 2 × 24 | 48 |
| Secret: research hut cache | 180 |
| Secondary objective | 150 |
| **Total** | **2,246** |

## Difficulty notes

- **Easy**: relay integrity 900; the whiteout peak lasts 6 s; the Stingers dive at the player
  instead of the relay.
- **Hard**: relay integrity 450; the t=118 cross has 2 Skimmers per edge; the t=210 Stingers
  dive in pairs.

## Concept art

No level-specific concept files. The look comes from the chosen arctic scene in
[art direction](../../../art-direction/README.md):
[scene-arctic-r09-a.png](../../../art-direction/concept/scene-arctic-r09-a.png) and its
[scroll loop](../../../art-direction/concept/scene-arctic-r09-a.gif), which already shows the
relay and Skimmers. The whiteout peak has no concept yet.

## Implementation

- [ ] Approach, halted defend phase and exit as in *Layout*; floes keep drifting while halted.
- [ ] Nansen Relay: integrity bar, damage only from relay-aimed attacks, hit flash, mission failed at 0.
- [ ] Target-the-objective configuration as in the table under *Nansen Relay*.
- [ ] Wave script matches the *Waves* table; edge warnings on all side and rear waves, also during the whiteout.
- [ ] Whiteout banks at the side edges, centre kept readable.
- [ ] Research hut secret and the two supply drones.
- [ ] Radio chatter cues incl. the integrity thresholds.
- [ ] Secondary objective (≥ 50% integrity) tracked and rewarded.
- [ ] Credit total at medium with perfect collection is 2,252 (± 5%).
- [ ] Easy/hard variations as in *Difficulty notes*.

## Decisions

- 2026-10-01: Promoted from the Act 2 roster to a draft level document.
- 2026-10-01: Primary `defend`: the relay destroyed means mission failed; failing the secondary
  objective only loses its bonus (user decision).
- 2026-10-01: The relay's spec moved to [allies](../../../allies/README.md#nansen-relay). Open question resolved: the per-unit relay-targeting overrides are replaced by the general target-the-objective hook in the enemies README, configured here as a small table.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
