---
title: Level 08 – Neon Skyline
design: draft
implementation: not-started
art: chosen
depends-on: [../../../player/wingmen, ../../../enemies/ground/creeper]
updated: 2026-10-01
---

# Level 08 – Neon Skyline

## Summary

The Act 2 opener. At night over **Nova Lagos**, Lancer flies low over the harbour, the elevated
highways and the tower district while Vrell walkers crawl over the rooftops. Rook is assigned
as Lancer's wingman and the escort slot opens. The level introduces the
[Creeper](../../../enemies/ground/creeper/README.md) and the megacity parallax with its traffic
lanes, and ends with a Creeper convoy on an elevated highway under a Needler circle. There is no
boss. About 3 minutes 20 seconds.

## Briefing

> **Commander Okafor:** "Lancer, Aegis Actual. While we were cleaning up at L1, a second fleet
> came in behind the debris. It made landfall in three places overnight, and Nova Lagos took
> the worst of it. Forty million people, and the Vrell are walking on their roofs. We fly low
> tonight: the harbour, the highways, the tower district. The walkers on the rooftops are new.
> They're slow, but they spray wide, so bring something that hits the ground. One more thing.
> Rook flies your wing from now on. He has five years in this outfit and he has never once
> lost a wingman. Don't be the first. Aegis Actual out."

*Hangar teaser* (shop screen before L08): "Nova Lagos is burning. Rook joins your wing: the
escort slot is open."

*Varga's intel line*: "Rooftop walkers, Lancer. Anti-ground fire hits them twice as hard. They
come straight down the avenues, so keep your guns forward."

## Threat profile

| Field | Value |
|---|---|
| Dominant layers | `ground` (Creepers, turret nests), `air`; `low-air` carries the traffic lanes (scenery, no enemies) |
| Attack directions | front 87% · sides 13% · rear 0% |
| Density | 3 |
| Recommended traits | `forward`, `anti-ground` |
| Hazards | none |
| Boss / mid-boss | none (finale: Creeper convoy plus Needler circle) |
| Sensor-suite detail | **none**: megacity, ground + air, front. **L1**: front 87 / sides 13, density 3, no hazards. **L2**: Skitter, Needler, Stinger, Creeper (new), Spine Turret and Polyp Mortar portraits; no boss. **L3**: `anti-ground` highlighted in the shop; the timeline strip marks the Creeper convoys at 84, 128 and 162 s; 1 secret. |

## Objective

- **Primary** `reach-end`: survive to the end of the scroll.
- **Secondary**: destroy all 12 Creepers before they leave the screen (they are heading for the
  Ikoyi shelters). Pays +90 credits and triggers a civilian line.

## Layout

Ground scroll speed **140 px/s** (a calm-to-normal city pace, as in the chosen megacity scene)
at the 960×540 baseline (play field 480×540, see [art direction](../../../art-direction/README.md)).
Total ≈ 200 s ≈ 28,000 px. Motion budget: the traffic lanes (sections 1–3) and the smoke
columns (sections 4–5) are the strongly animated elements, and the traffic thins out as the
smoke rises, so the two never run at full strength together.

| Section | t (s) | Scroll (px) | Speed (px/s) | Atmosphere | Layers and content | Purpose |
|---|---|---|---|---|---|---|
| 1. On the Wing | 0–20 | 0–2,800 | 140 | `light` (harbour mist) | `deep`: the Gulf of Guinea at night, the sprawl's lights to the horizon, haze wisps. `far`: lower city levels, harbour cranes. `ground`: Lagos harbour piers, container yards, the first elevated highway. `low-air`: thin mist banks, CDF gunships heading home. `air`: Rook glides into his Wing slot. `high-air`: a few smoke wisps. | Rook's formation and his firing shown with no enemies about. One HUD hint: "Rook's side can be set in the hangar". |
| 2. Traffic Lanes | 20–65 | 2,800–9,100 | 140 | `clear` | `far`: neon-lit lower levels. `ground`: two tree-lined avenues as lanes, elevated highways, rooftop landing pads, parks. `low-air`: streams of civilian aircars fleeing south (scenery: no collision, shots pass through), searchlights. | Rook shoots what the player shoots; the first rooftop turret nest. |
| 3. Rooftop Crawl | 65–115 | 9,100–16,100 | 140 | `light` | `ground`: dense tower roofs, rooftop gardens, water tanks, the walls and ledges the Creepers follow. `low-air`: thin fog banks lit amber by the avenue lamps; traffic thinning. `far`: avenue lamp lines. | Creeper introduction: one alone, then convoys. The billboard secret. |
| 4. Smoke District | 115–160 | 16,100–22,400 | 140 | `medium` | `deep`: fire glow under the haze. `ground`: burning blocks, a collapsed overpass, a parking deck. `low-air`: dense smoke columns (below the play plane). `high-air`: ash wisps (≤ 40% opacity). | Mixed waves, Stinger dives, mortars on the overpass. |
| 5. Neon Heights | 160–200 | 22,400–28,000 | 140 | `medium`, `heavy` peak 166–174 (smoke wall before the circle, 4 s ramps) | `far`: the Ndidi Arcology rising ahead with creep on its upper floors (the lead into L09). `ground`: arcology district, neon signage under smoke (no saturated teal), the elevated Third Mainland highway carrying the finale convoy. `low-air`: smoke banks. | Finale set piece, then the level ends over the arcology district. |

## Waves

Enemy definitions are linked per row; formation names come from the
[formation vocabulary](../../../enemies/README.md#formation-vocabulary). Returning Act 1 units
use the act HP factor from the [balancing basis](../../../enemies/README.md#balancing-basis); HP
values live in the enemy specs only.

| t (s) | Section | Formation | Enemies | Count | Enter from | Notes |
|---|---|---|---|---|---|---|
| 22 | 2 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 8 | front (left) | Curls between the avenues; Rook joins the player's target |
| 30 | 2 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 5 | front | |
| 38 | 2 | snake | Skitter | 8 | front (right) | Mirror of the first wave |
| 48 | 2 | line abreast | Needler | 3 | front | Hover over the highway, fire, leave |
| 58 | 2 | stream | Skitter | 8 | front (alternating edges) | |
| 68 | 3 | single | [Creeper](../../../enemies/ground/creeper/README.md) | 1 | front (rooftop) | **New.** Alone on a roof, crawls down a wall and across the next roof; radio cue from Varga |
| 84 | 3 | convoy | Creeper | 3 | front | Along a rooftop path; anti-ground does ×2 |
| 94 | 3 | pincer | Needler | 4 | sides | 2 per side; Rook switches to his Wide formation |
| 108 | 3 | V-wing | [Stinger](../../../enemies/air/stinger/README.md) | 3 | front | Dive one after another |
| 128 | 4 | convoy | Creeper | 3 | front | Through the burning blocks |
| 138 | 4 | snake | Skitter | 6 | front | Weaves through the smoke columns |
| 148 | 4 | column | Stinger | 3 | front | The last one drops the armour patch |
| 162 | 5 | convoy | Creeper | 5 | front | On the elevated highway; the first one drops an overdrive |
| 170 | 5 | circle | Needler | 6 | front | Orbits above the highway for 6 s, then breaks toward the player one by one |
| 176 | 5 | stream | Skitter | 6 | sides (both) | Enters while the circle orbits |

Totals: Skitter 36 · Needler 18 · Creeper 12 · Stinger 6.

## Ground targets

| Section | Target | Effect |
|---|---|---|
| 2 (t≈44) | [Spine Turret](../../../enemies/ground/spine-turret/README.md) turret nest ×3 on a rooftop pad | Credits; first ground threat of the act |
| 4 (t≈124) | Spine Turret turret nest ×3 on the parking deck | Credits |
| 4 (t≈150) | [Polyp Mortar](../../../enemies/ground/polyp-mortar/README.md) ×2 on the collapsed overpass | Credits; their impact markers teach "watch the ground" again |
| 3 (t≈100) | Flickering rooftop billboard ("LAGOS NEVER SLEEPS") | Secret, see below |

The civilian traffic on `low-air` is scenery and cannot be hit.

## Hazards

None. Smoke banks in sections 4–5 sit on `low-air`, below the play plane, and never hide
bullets.

## Secrets and pickups

- **Billboard cache** (t≈100): three hits topple the flickering billboard and uncover a CDF
  supply crate worth **160** credits. Rook: "*Finders keepers. Lagos owes us one anyway.*"
- **Overdrive**: dropped by the first Creeper of the t=162 convoy.
- **Armour patch**: dropped by the last Stinger of the t=148 column.
- Shield cells follow the normal drop table.

## Radio chatter

Text and a radio blip only (no voice acting). Rook's generic barks (rear, flank, low armour)
follow [his bark table](../../../player/wingmen/README.md#rook-ai-wingman); the scripted lines
are:

| Trigger | Speaker | Line |
|---|---|---|
| t=1 | Okafor | "Rook, you're on Lancer's wing as of now. Keep each other alive." |
| t=3 | Rook | "Copy, Actual. Lancer, I'm on your left. Don't make me look bad." (says "right" when set to the right) |
| t=18 | Okafor | "Contacts over the harbour district. Weapons free." |
| Rook's first kill | Rook | "Splash one. I'm keeping count, by the way." |
| t=44 | Rook | "Turrets on the rooftops. Mind the thorns." |
| t=66 | Varga | "Ground walker, rooftop level. It sprays wide. Get past it fast or hit it hard." |
| t=86 | Varga | "Bombs and mortars hit those walkers twice as hard." |
| t=92 | Rook | "Contacts on your flank!" |
| t=112 | Civilian | "Aegis, this is shelter nine in Ikoyi! They're on the roofs above us!" |
| t=113 | Okafor | "We hear you, shelter nine. Lancer, clear those roofs." |
| t=158 | Okafor | "Big convoy on the Third Mainland highway. Stop it before it reaches the shelters." |
| t=166 | The Choir | "[the Choir sings]" |
| t=167 | Varga | "That's the song from orbit. It's louder down here." |
| Level end | Okafor | "Good flying, both of you. Get some rest. The arcologies are next." |
| Secondary objective met | Civilian | "Shelter nine here. The roofs are quiet. Thank you, Aegis." |

## Boss / mid-boss

None. The Creeper convoy with the Needler circle (t=162–176) is the finale.

## Music & ambience

The Act 2 A theme "Homefront" (track 6 in the [track list](../../../audio/music/README.md#track-list)),
starting at the section 2 transition; the intensity stem comes in for the finale. Section 1 has
ambience only: the [megacity ambience](../../../audio/sfx/README.md#ambience-per-setting)
(distant sirens), engine hum and radio static.

## Credit budget

The total matches budget(8) = **1,606** from the
[economy](../../../systems/economy/README.md#per-level-budget) curve. Bounties are the spec values
(Act 1 terms) × the act factor 1.6, rounded per kill: Skitter 8, Needler 19, Stinger 24,
Creeper 35, Spine Turret 19, Polyp Mortar 24.

| Source | Credits (medium) |
|---|---|
| Kills: Skitter 36 × 8 + Needler 18 × 19 + Creeper 12 × 35 + Stinger 6 × 24 | 1,194 |
| Ground targets: Spine Turret 6 × 19 + Polyp Mortar 2 × 24 | 162 |
| Secret: billboard cache | 160 |
| Secondary objective | 90 |
| **Total** | **1,606** |

## Difficulty notes

- **Easy**: the t=94 pincer enters from the front instead; the t=84 convoy has 2 Creepers; one
  extra armour patch at t≈130.
- **Hard**: the t=84 and t=162 convoys have one more Creeper each; the Needler circle breaks
  toward the player in pairs; the overpass mortars get a pair of Spine Turrets.

## Concept art

No level-specific concept files. The look comes from the chosen megacity scene in
[art direction](../../../art-direction/README.md):
[parallax-r03-b.png](../../../art-direction/concept/parallax-r03-b.png) (shown at the `heavy`
end of the atmosphere range; this level runs mostly `clear` to `medium`).

## Implementation

- [ ] Scroll timeline, sections, atmosphere intensity and parallax content per layer as in *Layout*.
- [ ] Escort slot unlocked and Rook in formation from t=0 (story flag set before the L08 hangar visit).
- [ ] Civilian traffic lanes on `low-air` as non-colliding scenery.
- [ ] Wave script matches the *Waves* table (time, formation, count, entry edge).
- [ ] Ground targets and the billboard secret.
- [ ] Radio chatter cues fire at their triggers with portraits in the side HUD; Rook's side variant.
- [ ] Secondary objective (all 12 Creepers) tracked and rewarded.
- [ ] Credit total at medium with perfect collection is 1,606 (± 5%).
- [ ] Easy/hard variations as in *Difficulty notes*.

## Open questions

- Should the civilian traffic be hittable, with a penalty for hitting it? The draft keeps it as
  pure scenery so the player never has to hold fire.

## Decisions

- 2026-10-01: Promoted from the Act 2 roster to a draft level document.
- 2026-10-01: Rook joins as the player's wingman in the escort slot at L08 (user decision).
- 2026-10-01: Radio chatter is text plus radio blips only, no voice acting (user decision).
