---
title: Level 10 – Evacuation Corridor
design: draft
implementation: not-started
art: chosen
depends-on: [../../../enemies/air/wraith, ../../../enemies/air/mote-swarm, ../../../player/wingmen]
updated: 2026-10-01
---

# Level 10 – Evacuation Corridor

## Summary

The first **rear-heavy** level and the act's `escort` mission. Five civilian shuttles lift off
from Eko spaceport with eleven hundred evacuees, and Lancer and Rook fly the corridor with them
across the Nova Lagos outskirts to the lagoon, where they climb out. Cloaked
[Wraiths](../../../enemies/air/wraith/README.md) decloak behind the player and fire up the screen
through the shuttle column, and [Mote Swarm](../../../enemies/air/mote-swarm/README.md) flocks
sweep past and loop back from below. About 42% of the waves come from the bottom edge. One
shuttle loss is scripted for the story; every other loss is up to the player. About 3 minutes
20 seconds at a fast scroll; no boss.

## Briefing

> **Commander Okafor:** "Lancer, Aegis Actual. Nova Lagos is being evacuated. Five civilian
> shuttles are lifting from Eko spaceport with eleven hundred people aboard, and they need a
> corridor to the coast. You and Rook are that corridor. Stay with the shuttles until they reach
> the lagoon and climb out. Every shuttle we lose is two hundred lives. Varga has seen the Vrell
> hit evacuation flights from behind: cloaked flyers that only show when they fire, and flocks
> that sweep past and turn back on you. Watch your six. Fit a rear gun if you have one. Aegis
> Actual out."

*Hangar teaser* (shop screen before L10): "Evacuation escort. They hit from behind: rear weapons
recommended."

*Varga's intel line*: "Four in ten of their attacks will come from behind you, Lancer. The cloaked
ones decloak at the bottom of the screen. A rear gun or homing missiles will find them."

## Threat profile

| Field | Value |
|---|---|
| Dominant layers | `air`, `high-air` (cloaked Wraiths) |
| Attack directions | **rear focus**: front 53% · rear 42% · sides 5%. The hangar flags it as the first rear-heavy level. Mote Swarms count by the edge of their attack run (the bottom edge after the loop) |
| Density | 3 |
| Recommended traits | `rear`, `forward` (`homing` also hits cloaked Wraiths) |
| Hazards | none (the shuttle column takes enemy fire) |
| Boss / mid-boss | none (finale: Wraith rear ambush as the shuttles climb) |
| Sensor-suite detail | **none**: megacity outskirts, air, front and rear. **L1**: front 53 / rear 42 / sides 5, density 3, escort objective. **L2**: Wraith (new, cloaked), Mote Swarm (new), Needler, Stinger, Skitter portraits. **L3**: `rear` highlighted; the timeline strip marks the 8 rear waves; 1 secret. |

## Objective

- **Primary** `escort`: escort the five shuttles, **Lifeline One to Five**, to the orbital
  corridor at the end of the scroll.
  - **Lifeline Three** is lost in a scripted event at t≈118 (see below). Before that it takes
    no damage, so the scripted loss is always this shuttle and never doubles up with a
    player-caused loss.
  - **Fail**: all four other shuttles lost means mission failed (see [retry](../../../systems/retry/README.md)).
    Each loss before that only lowers the reward.
- **Secondary**: +40 credits per surviving shuttle among the four the player can save (up to
  +160).

### The shuttles

Five [evacuation shuttles](../../../allies/README.md#evacuation-shuttle) (size, armour 120,
damage and loss behaviour in the allies spec). Here they fly a loose double column in the band
y = 140–300, and each has a portrait frame with an armour bar in the HUD that flashes below 50%.

Because the rear attacks fire **up** the screen, they pass through the shuttle band. Killing the
rear threat quickly is what protects the convoy.

### Scripted loss (t≈118)

A violet glow pulses behind the low cloud deck for 2 s, high above the play field and on no
layer the player can hit. A thorn lance then drops onto Lifeline Three. The shuttle glides
down, smoking, into the `far` layer. The music ducks for 3 s and Okafor's line comes after a
beat. It cannot be prevented and costs no reward.

## Layout

Ground scroll speed **190 px/s** (a chase level per the
[art direction](../../../art-direction/README.md#parallax-layer-model)). Total ≈ 200 s ≈ 38,000 px.
Motion budget: the strong elements are the fast-moving ground and the smoke columns of
section 3; the refugee traffic on the roads is static or crawls.

| Section | t (s) | Scroll (px) | Speed (px/s) | Atmosphere | Layers and content | Purpose |
|---|---|---|---|---|---|---|
| 1. Liftoff | 0–20 | 0–3,800 | 190 | `light` (smoke) | `ground`: Eko spaceport pads, blast fences, service vehicles. The five shuttles lift off (ground → `low-air` → `air`, scaling up over 6 s). `low-air`: smoke from the city behind. `deep`: the sprawl at dusk. | The shuttles form up; no enemies. HUD shows the five shuttle portraits with armour bars. |
| 2. Refugee Roads | 20–70 | 3,800–13,300 | 190 | `clear` | `ground`: suburbs, highways jammed with stalled traffic and refugee columns (lights only, no figures at this scale), parks and football pitches. `low-air`: CDF helicopters heading the other way. | Mote Swarm and Wraith introductions, each in a safe situation first. |
| 3. The Corridor | 70–130 | 13,300–24,700 | 190 | `medium` | `ground`: a maglev viaduct between two infested districts, creep on both sides. `low-air`: smoke columns. `deep`: a violet glow moving in the cloud deck (the lander, foreshadowing the loss). `high-air`: smoke wisps (≤ 40%). | Rear pressure builds; the scripted loss. |
| 4. Coast Road | 130–175 | 24,700–33,250 | 190 | `light`, `medium` from 160 (sea mist) | `ground`: the lagoon shore, fishing piers, sandbars, a capsized ferry. `low-air`: sea-mist banks rolling in from the right. | Mixed front and rear waves; the ferry secret. |
| 5. Orbital Corridor | 175–200 | 33,250–38,000 | 190 | `clear` | `deep`: open lagoon and sea, the evening sky. `ground`: shallow water, buoys. The shuttles' engines flare and they climb off the top of the screen at t≈198. | Finale and release. |

## Waves

Enemy definitions are linked per row; formation names come from the
[formation vocabulary](../../../enemies/README.md#formation-vocabulary). Returning units use the
act HP factor from the [balancing basis](../../../enemies/README.md#balancing-basis); HP values
live in the enemy specs only. Every rear wave gets the 1.5 s edge warning, and Rook switches to
his Trail formation (see [Rook's AI](../../../player/wingmen/README.md#rooks-ai-first-draft-parameters)).

| t (s) | Section | Formation | Enemies | Count | Enter from | Notes |
|---|---|---|---|---|---|---|
| 24 | 2 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 5 | front | |
| 34 | 2 | swarm | [Mote Swarm](../../../enemies/air/mote-swarm/README.md) | 12 | front | **New.** Safe first sight: sweeps down the left lane and leaves at the bottom-left without looping back |
| 44 | 2 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 8 | front | |
| 52 | 2 | rear ambush | [Wraith](../../../enemies/air/wraith/README.md) | 1 | rear | **New.** A single Wraith, edge warning plus Rook's call; its bursts pass the shuttle band |
| 62 | 2 | swarm → rear ambush | Mote Swarm | 16 | rear (loop) | The full pattern: enters at the top, exits bottom-right, re-enters from the bottom after 1.5 s |
| 76 | 3 | V-wing | [Stinger](../../../enemies/air/stinger/README.md) | 4 | front | A rear gun catches them after the dive |
| 82 | 3 | rear ambush | Wraith | 2 | rear | |
| 92 | 3 | swarm → rear ambush | Mote Swarm | 20 | rear (loop) | Dives up through the shuttle column |
| 100 | 3 | stream | Skitter | 10 | front (alternating edges) | |
| 110 | 3 | rear ambush | Wraith | 2 | rear | The second one drops an overdrive |
| 118 | 3 | — | scripted | — | — | Lifeline Three lost |
| 120 | 3 | line abreast | Needler | 3 | front | |
| 130 | 4 | column | Stinger | 4 | front | |
| 140 | 4 | rear ambush (two swarms) | Mote Swarm | 2 × 10 | rear (bottom-left and bottom-right) | Crossing swarms |
| 146 | 4 | pincer | Needler | 4 | sides | Rook switches to Wide |
| 152 | 4 | stream | Skitter | 8 | front | |
| 158 | 4 | rear ambush | Wraith | 3 | rear | The last one drops an armour patch |
| 166 | 4 | snake | Skitter | 6 | front | |
| 182 | 5 | circle | Needler | 6 | front | Orbits ahead of the shuttles, then breaks one by one |
| 188 | 5 | rear ambush | Wraith | 3 | rear | Decloaks as the shuttles start to climb |

Totals: Needler 18 · Skitter 32 · Stinger 8 · Mote Swarm 68 motes · Wraith 11. Of 19 waves, 10
enter from the front, 8 from the rear and 1 from the sides.

## Ground targets

| Section | Target | Effect |
|---|---|---|
| 4 (t≈150) | Capsized ferry on a sandbar, its hull hatch blinking | Secret, see below |

No hostile ground targets: the threat in L10 is all in the air.

## Hazards

None. The danger is to the shuttle column: enemy fire aimed at the player from below crosses
the shuttle band.

## Secrets and pickups

- **Ferry cache** (t≈150): four hits on the capsized ferry's blinking hatch blow it open and
  release a CDF supply crate worth **160** credits.
- **Overdrive**: dropped by the second Wraith of the t=110 ambush.
- **Armour patch**: dropped by the last Wraith of the t=158 ambush.
- Shield cells follow the normal drop table.

## Radio chatter

| Trigger | Speaker | Line |
|---|---|---|
| t=1 | Civilian (Lifeline One) | "Eko Control, Lifeline One. Five birds, eleven hundred souls. We're going." |
| t=3 | Okafor | "Lifeline flight, Aegis has you. Stay in the corridor and stay together." |
| t=32 | Rook | "Something's coming down the left. A lot of somethings." |
| t=36 | Varga | "That's a flock, not a formation. They steer like starlings." |
| t=50 | Rook | "Six o'clock, Lancer!" |
| t=54 | Varga | "It was invisible until it fired! Only homing and beam weapons touch it while it's cloaked. Rear guns, Lancer." |
| t=60 | Rook | "The flock's turned around. They're coming up behind us!" |
| First shuttle damage | Civilian (hit shuttle) | "Something hit our tail! We're still flying. Please stay close!" |
| t=116 | Civilian (Lifeline Three) | "Aegis, there's a light above the clouds. What is that—" |
| t=119 | Rook | "Where did that come from? Nothing on my scope!" |
| t=121 | Okafor | "Lifeline Three is down." (1.5 s pause) "Keep the others moving, Lancer." |
| t=150 | Varga | "That shot came from above the cloud deck. Something big is sitting up there, out of reach." |
| A shuttle is lost (player) | Okafor | "We lost Lifeline [n]. Stay on the others." |
| t=180 | The Choir | "[the Choir sings]" |
| t=196 | Rook | "Last one out of Lagos, get the lights." |
| t=198 | Civilian (Lifeline One) | "Corridor clear. We can see the sky. Thank you, Aegis. Thank you." |
| Level end | Okafor | "[n] shuttles made orbit. That's [n × 220] people who'll see tomorrow. Good work." |
| Secondary objective met (all four) | Okafor | "Four of five. Remember the ones you got home, Lancer." |

## Boss / mid-boss

None. The Wraith ambush under the climbing shuttles (t=188) is the finale.

## Music & ambience

The Act 2 A theme "Homefront" (track 6 in the [track list](../../../audio/music/README.md#track-list)),
starting at the section 2 transition. The intensity stem comes in from t=82. At the scripted loss
the music ducks −6 dB for 3 s with no sting. Ambience: the
[megacity ambience](../../../audio/sfx/README.md#ambience-per-setting) crossfading to the ocean
ambience in section 4.

## Credit budget

The total is within 0.3% of budget(10) = **1,838** from the
[economy](../../../systems/economy/README.md#per-level-budget) curve. Bounties are the spec values ×
the act factor 1.6, rounded per kill: Skitter 8, Needler 19, Stinger 24, Mote 3, Wraith 48.

| Source | Credits (medium) |
|---|---|
| Kills: Needler 18 × 19 + Skitter 32 × 8 + Stinger 8 × 24 + Mote 68 × 3 + Wraith 11 × 48 | 1,522 |
| Ground targets | 0 |
| Secret: ferry cache | 160 |
| Secondary objective: 4 shuttles × 40 | 160 |
| **Total** | **1,842** |

## Difficulty notes

- **Easy**: shuttle armour 180; the t=110 and t=158 Wraith ambushes have one Wraith fewer; Mote
  Swarms make one dive and then leave.
- **Hard**: shuttle armour 90; an extra rear ambush of 2 Wraiths at t=170; Mote Swarms loop
  round for a second dive.

## Concept art

No level-specific concept files. The look comes from the chosen megacity scene in
[art direction](../../../art-direction/README.md):
[parallax-r03-b.png](../../../art-direction/concept/parallax-r03-b.png); the coast road
borrows the water treatment of the chosen ocean scene
([scene-ocean-r10-a.png](../../../art-direction/concept/scene-ocean-r10-a.png)). The shuttle
sprite has no concept yet (see [allies](../../../allies/README.md)).

## Implementation

- [ ] Scroll timeline, sections, atmosphere intensity and parallax content per layer as in *Layout*.
- [ ] Shuttle escort units: formation band, armour, damage from enemy fire, smoke and glide-down loss, HUD portraits with armour bars.
- [ ] Scripted loss of Lifeline Three at t≈118 (invulnerable before it; glow telegraph; lance; music duck).
- [ ] Mission failed when all four non-scripted shuttles are lost.
- [ ] Wave script matches the *Waves* table; edge warnings on all rear waves.
- [ ] Ferry secret.
- [ ] Radio chatter cues incl. the dynamic shuttle count in the level-end line.
- [ ] Secondary objective (+40 per surviving shuttle) tracked and rewarded.
- [ ] Credit total at medium with perfect collection is 1,838 (± 5%).
- [ ] Easy/hard variations as in *Difficulty notes*.

## Decisions

- 2026-10-01: Promoted from the Act 2 roster to a draft level document.
- 2026-10-01: One scripted shuttle loss for the story; every other loss depends on the player,
  and losing all of them fails the mission (user decision).
- 2026-10-01: Open question resolved: friendly units got their own part, [allies](../../../allies/README.md); the shuttle spec moved there. Sprites come in a later concept round.
