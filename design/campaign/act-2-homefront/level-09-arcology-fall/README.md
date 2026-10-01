---
title: Level 09 – Arcology Fall
design: review
implementation: not-started
art: chosen
depends-on: [../../../enemies/ground/hive-node, ../../../enemies/ground/ravager]
updated: 2026-10-01
---

# Level 09 – Arcology Fall

## Summary

The first `destroy-targets` mission. The Vrell have planted six **hive nodes** in three
clusters across the arcology district of Nova Lagos, and every node breeds swarmers until it
dies. The nodes are hardened, so the level is the first that *needs* an `anti-ground` source.
Ravager packs, new animal-like pack hunters, gallop through the ruined streets and pounce at the
player. When the last cluster dies, the creep-hollowed Ndidi Arcology collapses. About 3 minutes
20 seconds; no boss.

## Briefing

> **Commander Okafor:** "Lancer, Aegis Actual. Overnight the Vrell planted hive nodes in the
> arcology district. Six of them, in three clusters. Each one breeds swarmers every few seconds,
> and the creep they spread is eating the towers from the inside. Your objective: destroy all six
> nodes. If even one survives, we lose the district. The nodes are hardened, so standard rounds
> will glance off. Fit bombs or mortars, or bring Hammer flight. Recon also reports something
> new on the streets. It's fast and low and it moves in packs. Watch the ground as well as the
> sky. Aegis Actual out."

*Hangar teaser* (shop screen before L09): "Six hive nodes in the arcologies. Hardened: bring
anti-ground."

*Varga's intel line*: "The nodes are armoured mounds, Lancer. Only anti-ground weapons crack
them, or the Airstrike. No anti-ground, no mission."

## Threat profile

| Field | Value |
|---|---|
| Dominant layers | `ground` (hive nodes, Ravagers, Creepers, turrets), `air` |
| Attack directions | front 89% · sides 11% · rear 0% |
| Density | 3 |
| Recommended traits | `anti-ground` (**required** for the primary objective), `area` |
| Hazards | Arcology collapse (scripted, on the ground layer, harmless to the player) |
| Boss / mid-boss | none |
| Sensor-suite detail | **none**: megacity, ground + air, front. **L1**: front 89 / sides 11, density 3, hazard "structural collapse". **L2**: Hive Node (new, hardened), Ravager (new), Creeper, Needler, Spine Turret portraits. **L3**: `anti-ground` and `area` highlighted; the timeline strip marks the three node clusters (≈40, ≈124, ≈172 s); 1 secret. |

Launching without any `anti-ground` source (weapon, Rook's Mortar or the Airstrike) shows a
launch warning in the hangar (generic missing-trait rule in [hangar](../../../ui/hangar/README.md)).

## Objective

- **Primary** `destroy-targets`: destroy the six named hive nodes **A1, A2** (Unity Plaza),
  **B1, B2** (Okonjo Bridge, south bank) and **C1, C2** (base of the Ndidi Arcology).
  - **Hold zones**: when a cluster's first node reaches y = 200 the scroll eases down to
    30 px/s over 1 s. It eases back to 150 px/s when the cluster is destroyed, or after 15 s.
  - A node that leaves the bottom edge alive is **missed**: the HUD flags it, Okafor calls it,
    and it keeps spawning off-screen. Its Skitters arrive as a rear `stream` (2 every 4 s) for
    the rest of the level.
  - **Fail**: any node still alive when the scroll ends means mission failed (the hive takes
    the district), per the campaign default for `destroy-targets`. See [retry](../../../systems/retry/README.md).
- **Secondary**: kill all 8 Ravagers of the bridge assault (t=108 and t=116) before any reach
  the Kilo truck convoy at mid-bridge. Pays +100 credits.

## Layout

Ground scroll speed **150 px/s** (normal), easing to **30 px/s** in the hold zones. Times below
assume a typical 6 s hold per cluster. **Waves after a hold are triggered by scroll distance,
not by time**, so a longer hold delays the rest of the script instead of piling waves up.
Total ≈ 200 s ≈ 27,840 px (up to ≈ 27 s longer if every hold runs its full 15 s). Motion
budget: the strong elements are the burning towers' smoke and, in section 5, the collapse dust;
the creep on the towers pulses slowly.

| Section | t (s) | Scroll (px) | Speed (px/s) | Atmosphere | Layers and content | Purpose |
|---|---|---|---|---|---|---|
| 1. Arcology Approach | 0–20 | 0–3,000 | 150 | `light` | `deep`: the sprawl under a smoke haze, dawn grey in the east. `far`: arcology towers with violet creep veins on their upper floors. `ground`: a ruined elevated highway, burnt-out traffic, a CDF barricade. `low-air`: smoke wisps. | Objective set-up; the first creep seen on the towers. |
| 2. Unity Plaza | 20–62 | 3,000–8,580 | 150 / 30 in the hold | `clear` | `ground`: a wide plaza with a dry fountain, the creep spreading from nodes A1/A2 across the paving, park trees at the edges, two turrets. `low-air`: none (the plaza must read clearly). | First hold zone: learn that nodes need anti-ground and that the scroll waits for you. |
| 3. Boulevard Run | 62–100 | 8,580–14,280 | 150 | `light` (dust) | `ground`: a long boulevard with a burnt-out tram, abandoned cars, tree rows, rooftops either side. `low-air`: dust drifting off the street. `far`: tower canyons. | Ravager introduction: one pack alone, then a larger pack around the tram. |
| 4. Okonjo Bridge | 100–146 | 14,280–20,460 | 150 / 30 in the hold | `medium` (river smoke) | `ground`: the lagoon channel, the Okonjo Bridge running up the screen with the CDF Kilo truck convoy crossing it, nodes B1/B2 on the south-bank approach. `low-air`: smoke banks over the water. | Second hold; the bridge assault (secondary objective). |
| 5. Arcology Fall | 146–200 | 20,460–27,840 | 150 / 30 in the hold | `medium`, `heavy` dust peak for 6 s after the collapse, then `light` | `far`: the Ndidi Arcology filling the top of the screen, hollowed by creep. `ground`: its lobby plaza with nodes C1/C2, collapsing skybridges. After the collapse: rubble field, dust banks on `low-air`. | Third hold, then the collapse set piece. |

**Collapse set piece**: when C1 and C2 are both destroyed, the arcology's shadow sweeps across
the ground layer for 1.5 s (the warning), then the tower falls across the ground layer from left
to right over 3 s. Ground enemies under it are destroyed and pay their bounty. The player and
air enemies are unaffected; a `heavy` dust peak follows, ramping out over 6 s. If C1 or C2 is
missed, the tower falls on a timer at t≈190 and the mission still fails at the end of the scroll.

## Waves

Enemy definitions are linked per row; formation names come from the
[formation vocabulary](../../../enemies/README.md#formation-vocabulary). Returning units use the
act HP factor from the [balancing basis](../../../enemies/README.md#balancing-basis); HP values
live in the enemy specs only. Skitters spawned by the hive nodes are listed under *Ground
targets*.

| t (s) | Section | Formation | Enemies | Count | Enter from | Notes |
|---|---|---|---|---|---|---|
| 12 | 1 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 5 | front | Over the ruined highway |
| 28 | 2 | convoy | [Creeper](../../../enemies/ground/creeper/README.md) | 3 | front | Crawls across the plaza toward the nodes |
| 66 | 3 | pack | [Ravager](../../../enemies/ground/ravager/README.md) | 3 | front | **New.** Gallops down the empty boulevard and pounces once within 200 px; radio cue |
| 80 | 3 | pack | Ravager | 4 | front | Splits around the burnt-out tram; the last one drops an overdrive |
| 92 | 3 | line abreast | Needler | 3 | front | |
| 108 | 4 | pack | Ravager | 4 | front (far bank) | Bridge assault, part 1: heads for the truck convoy |
| 116 | 4 | pack | Ravager | 4 | front (far bank) | Bridge assault, part 2 |
| 134 | 4 | pincer | Needler | 4 | sides | |
| 160 | 5 | pack | Ravager | 3 | front | Charges out of the arcology lobby |

Totals: Needler 12 · Ravager 18 · Creeper 3 · plus about 12 node-spawned Skitters.

## Ground targets

| Section | Target | Effect |
|---|---|---|
| 2 (t≈40) | [Hive Node](../../../enemies/ground/hive-node/README.md) **A1, A2** (`turret nest`), hardened | Primary targets. Each spawns 2 [Skitters](../../../enemies/air/skitter/README.md) every 4 s while alive |
| 2 (t≈36) | [Spine Turret](../../../enemies/ground/spine-turret/README.md) ×2 guarding the plaza | Credits; the first one drops an Airstrike special charge pickup |
| 4 (t≈124) | Hive Node **B1, B2**, hardened | Primary targets |
| 4 (t≈120) | Spine Turret ×2 at the bridgehead | Credits |
| 5 (t≈172) | Hive Node **C1, C2**, hardened | Primary targets; destroying both triggers the collapse |
| 3 (t≈88) | Creep cocoon on a rooftop, hardened like a node | Secret, see below |

The budget assumes each node spawns once (2 Skitters) before it dies: 12 Skitters. A player who
lets nodes live earns a little more from the spawns, capped by the 15 s hold.

## Hazards

- **Arcology collapse** (section 5): ground layer only; it destroys ground enemies, never the
  player.
- No terrain collision; smoke and dust banks sit on `low-air`.

## Secrets and pickups

- **Rooftop cocoon** (t≈88): a creep cocoon on a roof, hardened (anti-ground or the Airstrike
  only). Breaking it frees a CDF supply crate worth **160** credits plus an armour patch.
- **Airstrike charge**: dropped by the first plaza Spine Turret. It tops up a player who brought
  the Airstrike; for anyone else it is a shield cell.
- **Overdrive**: dropped by the last Ravager of the t=80 pack.

## Radio chatter

| Trigger | Speaker | Line |
|---|---|---|
| t=2 | Okafor | "Hive nodes confirmed in the arcology district. Six of them. All six die, Lancer." |
| t=4 | Rook | "Six bugs, two pilots. I like those odds." |
| t=30 | Varga | "The nodes are hardened. Normal rounds will just spark off. Use bombs or mortars, or call Hammer flight." |
| Hold zone starts (first) | Okafor | "Slowing you down over the plaza. Make it count." |
| First node destroyed | Rook | "One down. That's a horrible noise they make." |
| t=64 | Varga | "Fast movers on the ground! Low, four-legged... they're hunting." |
| First Ravager pounce | Rook | "It jumped at me! Since when do they jump?" |
| t=104 | CDF officer | "Aegis, Kilo convoy on the Okonjo Bridge. Sixty civilians in the trucks, and things are coming over the far bank." |
| t=106 | Okafor | "Lancer, keep them off that bridge." |
| A node is missed | Okafor | "A node got past you, Lancer. If it lives, the district is lost." |
| t=150 | Varga | "The arcology's frame is hollow with creep. Kill those last nodes and it may come down." |
| t=158 | The Choir | "[the Choir sings]" |
| Cluster C destroyed | Rook | "Timber. Somebody had to say it." |
| Level end | Okafor | "All six nodes dead. The outer districts evacuate at first light, and you're flying cover." |
| Secondary objective met | CDF officer | "Kilo convoy is across. Thank you, Aegis." |

## Boss / mid-boss

None. The third hold zone and the collapse are the finale.

## Music & ambience

The Act 2 B theme "Firestorm" (track 7 in the [track list](../../../audio/music/README.md#track-list));
the intensity stem comes in during each hold zone. Ambience: the
[megacity ambience](../../../audio/sfx/README.md#ambience-per-setting) with distant sirens; the
collapse is a low rumble swelling to a long crash (SFX, not a music sting).

## Credit budget

The total is within 0.1% of budget(9) = **1,718** from the
[economy](../../../systems/economy/README.md#per-level-budget) curve. Bounties are the spec values ×
the act factor 1.6, rounded per kill: Skitter 8, Needler 19, Creeper 35, Ravager 29, Hive Node
72, Spine Turret 19.

| Source | Credits (medium) |
|---|---|
| Kills: Needler 12 × 19 + Ravager 18 × 29 + Creeper 3 × 35 + node-spawned Skitter 12 × 8 | 951 |
| Ground targets: Hive Node 6 × 72 + Spine Turret 4 × 19 | 508 |
| Secret: rooftop cocoon | 160 |
| Secondary objective | 100 |
| **Total** | **1,719** |

## Difficulty notes

- **Easy**: hold zones last up to 25 s; the bridge assault packs have 3 Ravagers each (the
  secondary objective counts 6); a second Airstrike charge pickup at the bridgehead.
- **Hard**: hold zones last up to 10 s; the bridge assault packs have 5 Ravagers each (the
  secondary counts 10); cluster C is guarded by a Spine Turret pair.

## Concept art

No level-specific concept files. The look comes from the chosen megacity scene in
[art direction](../../../art-direction/README.md):
[parallax-r03-b.png](../../../art-direction/concept/parallax-r03-b.png). The creep and the hive
node follow the chosen [Hive Node](../../../enemies/ground/hive-node/README.md) concept.

## Implementation

- [ ] Scroll timeline, sections, atmosphere intensity and parallax content per layer as in *Layout*.
- [ ] Hold zones: scroll eases to 30 px/s per cluster, resumes on cluster death or after 15 s; waves after a hold keyed to scroll distance.
- [ ] Named targets A1–C2 tracked in the HUD; missed nodes flagged and spawning a rear Skitter stream.
- [ ] Mission failed at the end of the scroll if any node is alive.
- [ ] Wave script matches the *Waves* table.
- [ ] Arcology collapse set piece (shadow warning, fall, ground kills, dust peak).
- [ ] Rooftop cocoon secret and the Airstrike charge pickup.
- [ ] Radio chatter cues fire at their triggers.
- [ ] Secondary objective (bridge assault) tracked and rewarded.
- [ ] Credit total at medium with perfect collection is 1,718 (± 5%).
- [ ] Easy/hard variations as in *Difficulty notes*.

## Decisions

- 2026-10-01: Promoted from the Act 2 roster to a draft level document.
- 2026-10-01: Primary `destroy-targets`: a node alive at the end of the scroll fails the mission;
  failing the secondary objective only loses its bonus (user decision).
- 2026-10-01: L09 introduces the Ravager (round 05 follow-up).
- 2026-10-01: The "no anti-ground source" launch warning is covered by the generic hangar rule (confirmation when a recommended trait is missing; `anti-ground` is recommended here) — see [hangar](../../../ui/hangar/README.md).
- 2026-10-01: A missed node triggers the immediate lost-mission prompt (campaign rule), instead of failing only at the end of the scroll.
