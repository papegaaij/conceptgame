---
title: Story
design: draft
implementation: n/a
art: chosen
updated: 2026-09-30
---

# Story

## Summary

In 2185 an ancient structure beyond Neptune, the Tether Gate, wakes up and the biomechanical
Vrell pour into the solar system. The player is "Lancer", a pilot of the Coalition Defence
Force's Aegis Wing, flying the AF-12 Stormhawk from Earth orbit to the far side of the gate.
Halfway through the war it turns out the gate did not open by itself: the Jovian Ascendancy
opened it, trading humanity for alien biotech.

## Contents

| Part | Summary | Design | Impl | Art |
|---|---|---|---|---|
| [factions](factions/README.md) | The three powers: Coalition, Vrell, Jovian Ascendancy | draft | n/a | none |
| [characters](characters/README.md) | Briefing and radio cast, including the player | draft | n/a | proposed |
| [twist](twist/README.md) | The mid-campaign twist: three variants to choose from | approved | n/a | n/a |

## Design

### Premise

Humanity has spread across the solar system without ever meeting anyone else. The United
Terran Coalition (UTC) holds Earth, Luna, Mars, Europa and the belt stations together; the
Jovian cloud cities are nominally Coalition but increasingly go their own way. Its military,
the Coalition Defence Force (CDF), is a peacetime force built for piracy and border disputes.

On 14 March 2185 the Tether Gate — a ring the size of a small moon, drifting beyond Neptune's
orbit and studied for decades as a dead relic — lights up. Eleven days later Vrell swarms hit
the outer stations. Six weeks later they are in Earth orbit.

The player is **Lancer**, a young pilot of the CDF 7th Interceptor Wing, "Aegis Wing", flying
the new AF-12 Stormhawk. Aegis Wing becomes the Coalition's fire brigade: sent wherever the
line is breaking.

### History, 2050–2185

| Year | Event |
|---|---|
| 2051 | First permanent lunar base, Shackleton Station. |
| 2068 | Fusion torch drives make the inner system a months-long trip instead of years. |
| 2079 | Ares Landing, the first Mars colony. Terraforming domes follow in the 2090s. |
| 2102 | United Terran Coalition founded to govern Earth and the off-world colonies together. |
| 2114 | Belt mining boom; hundreds of rock-hollowed stations, chartered to mining corporations. |
| 2127 | Europa's Thera Deep: a pressure-dome city under the ice, farming the ocean. |
| 2140 | First Jovian cloud city, Aurelia, floats in Jupiter's upper atmosphere, harvesting helium-3. |
| 2141 | The Tether Gate is discovered beyond Neptune. Dead, unexplained, ancient. |
| 2150s | Helix Dynamics builds its empire on Jovian helium-3 and funds most gate research. |
| 2166 | Jovian tax riots. The Coalition sends the CDF; eleven dead in Aurelia. Nobody out there forgets. |
| 2171 | The Jovian Ascendancy movement is founded, chaired by Helix CEO Silas Vorne. |
| 2179 | A Helix deep-survey mission to the gate "goes missing". Officially an accident. |
| 2183 | The Ascendancy wins Jovian autonomy votes; Coalition relations freeze. |
| 2185 | The Tether Gate activates. The war begins. |

### The war, act by act

Acts, levels and bosses are defined in [campaign](../campaign/README.md); settings in
[world](../world/README.md).

| Act | Story beat |
|---|---|
| 1 First Contact | Vrell swarms hit Earth orbit and Luna. Aegis Wing survives the first battles and learns how the Vrell fight. Ends by destroying the Brood Carrier at the Earth–Moon L1 point. |
| 2 Homefront | Vrell landers break through to Earth's surface. Fighting over megacities, oceans and the arctic. The Siege Spire, a living fortress rooted in a city, is torn out. Earth holds. |
| 3 Red Dust | Mars is under siege. The Vrell act strangely here: they avoid Helix facilities. Varga notices; Okafor tells her to keep it quiet. Then come unmarked human-built drones (L18) and a walker made of Helix alloy (L19). Boss: the Dust Colossus. |
| 4 Deep Water | Under Europa's ice the Vrell are farming something. Aegis Wing finds human-built relay beacons guiding Vrell ships — black and gold — and Varga confirms a Helix hull code (L27). Rook is shot down and goes missing (L26). After the Abyssal Maw falls, Ascendancy ships open fire on Aegis Wing (L28). |
| 5 The Belt | The reveal: the Jovian Ascendancy opened the gate. Vorne's broadcast to the system opens the act (L29). Ascendancy and Vrell fight side by side; the belt stations choose sides, and Rook is freed at Ceres Hub. Boss: the Iron Sovereign battle station. |
| 6 Jovian Storm | The hunt for Vorne through Jupiter's storms and cloud cities to the Ascendancy HQ on Callisto. The Vrell turn on the Ascendancy mid-act (L37, Aurelia) — they were never partners, only a door. In Helix HQ (L41) Varga finds Vorne's deal: the Vrell wanted passage because they are fleeing something the files call "the Silence" — its first mention. Boss: Vorne's flagship Ascendant; Vorne escapes toward the gate (L42). |
| 7 Beyond the Gate | Through the Tether Gate after Vorne and into Vrell space to silence the Choir. On the far side: dead Vrell worlds and more hints of the Silence. Vorne, absorbed by the Choir, dies at L48. Final boss: the Choir Heart. Epilogue: the gate goes dark — but it was built to keep something out. |

### Tone guide: serious with pulp edges

The war is real, losses matter, and nobody winks at the camera. On top of that, the characters
are big and memorable, the villain enjoys himself, and a well-placed one-liner is allowed.

Do:
- Let characters react to losses: a pause, a short line, then back to work.
- Give Vorne theatrical speeches and give Rook dry jokes — sparingly, one per level at most.
- Keep briefings military: objective, threat, one line of context, one line of character.
- Make the Vrell alien, not evil: they do not taunt, they sing.
- Use concrete, grounded detail (station names, casualty counts, ship classes).

Don't:
- Parody, fourth-wall breaks, memes, or modern slang.
- Long cutscene monologues; a briefing is readable in 20 seconds.
- Gore. Destruction is shown through ships and cities, not bodies.
- Make the player character speak. Lancer is silent; others talk *to* Lancer.

### How the story is told

- **Act briefing** (start of every act): a full-screen briefing by Okafor with a still image of
  the setting and 4–6 short paragraphs. At the end of an act, a short debrief.
- **Mission briefing** (before every level, after the hangar): portrait, 2–4 lines of text,
  objective, and a threat summary taken from the level's threat profile. Skippable.
- **Radio chatter** (in-level): a portrait and one line of text in the side HUD panel, shown for
  about 3 seconds, triggered by level events (wave start, boss appears, low armour, wingman
  down). Never pauses the game. At most one message on screen; low-priority lines are dropped.
- **Hangar intel**: Varga's comments on the next level, next to the intel panel.
- **Enemy transmissions**: Vorne and the Choir break into the radio with their own portrait
  frames (static-distorted), usually at boss fights.

The briefing screen and HUD layout are specified in [ui](../ui/README.md).

### Sample mission briefing (tone example)

The binding level 01 briefing is in [level 01](../campaign/act-1-first-contact/level-01-break-at-dawn/README.md);
this sample only sets the voice.

> **OKAFOR:** Lancer, this is not a drill. Unknown contacts have crossed the lunar orbit and
> hit the Gagarin shipyards. Whatever they are, they are fast, and there are a lot of them.
>
> Your job: get up there with Aegis Wing and keep them off the shipyards until the evacuation
> shuttles are clear.
>
> **OBJECTIVE:** Survive until the shuttles are away. **THREAT:** Swarms, from the front.

### Sample radio lines

| Speaker | Line |
|---|---|
| Okafor | "Aegis, hold the line. Nobody gets past you." |
| Okafor | "That's the last of them. Good work. Come home." |
| Rook | "Contacts on six! Why is it always six?" |
| Rook | "Lancer, you still with me? Say something. Or, you know, shoot something." |
| Varga | "Their armour regrows. Hit the glowing sacs, not the shell." |
| Varga | "Those beacons are human-made. Commander, those are Helix frequencies." |
| Vorne | "Such dedication. Such waste. You could have been on the winning side, pilot." |
| The Choir | "…the door is open… we are many… we are going…" |

## Open questions

- Which twist variant to use (see [twist](twist/README.md)). All other documents assume
  variant A.
- Vorne's death: in Act 7 (L48, absorbed by the Choir; current) or at the end of Act 6 with his
  flagship (the earlier draft)? Act 7 gives the fleet a reason to cross the gate and the act a
  villain finale.
- Is there an ending variation (e.g. based on difficulty or secrets found), or one ending?
- Voice acting for radio lines, or text only (with a radio-static "blip" sound)?

## Decisions

- 2026-09-30: Alien invasion with a mid-campaign twist, serious tone with pulp edges, told via
  briefings and in-level radio chatter.
- 2026-09-30: Lore names fixed for all documents (UTC/CDF, Vrell, Jovian Ascendancy, Tether
  Gate, cast). Timeline kept in this README instead of a separate `timeline/` part.
- 2026-09-30: Arc aligned with the campaign: Vorne escapes at L42 and dies at L48 (was: dies at
  the end of Act 6); the Silence is first named at L41 in Vorne's files; Rook is missing
  L27–L29; Vorne's broadcast opens Act 5.
