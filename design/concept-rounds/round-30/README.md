---
title: Concept round 30 — M5 part B, Level 08 and the Act 2 intro
design: approved
implementation: n/a
art: chosen
depends-on: [../../campaign/act-2-homefront, ../../campaign/act-2-homefront/level-08-neon-skyline, ../../enemies/ground/creeper, ../../art-direction, ../../ui/briefing, ../../ui/hangar, ../../story/characters/rook, ../../audio/music, ../../audio/voice]
updated: 2026-10-07
---

# Concept round 30 — M5 part B, Level 08 and the Act 2 intro

**Closed 2026-10-07** (user): everything accepted, with the billboard **a**, the traffic **a** and
the civilian's voice **b**; the one-screen page rule and the teaser's role labels kept. The outcome
of each row is in the *Choices* table.

## Summary

The review round of M5 part B ([roadmap](../../tech/roadmap/README.md#m5-parts)): Level 08
*Neon Skyline* and the Act 1 → Act 2 transition. The production art went straight to production
(D6 = a) and is reviewed here as **final**:
- the Creeper, with its readability lift and intel portrait;
- Level 08's megacity backdrop with its perspective towers;
- the Act 2 title-card still;
- the six briefing images;
- Rook's briefing portraits;
- "Homefront" and its base stem.

Three a/b picks are new: the billboard, the traffic, and the Ikoyi shelter civilian's voice. The
round also holds:
- the game captures of Level 08 and the transition, with the readability pass after them;
- a listen to the 19 new voiced lines;
- the rewritten texts (D3), quoted in full;
- the numbers the agents chose or measured;
- a checklist of the user's part B decisions;
- two rules to confirm.

Open [index.html](index.html) in a browser (regenerate with `python3 tools/concept/board.py 30`).

Every art row is *approve as final* or *redo* (say what to change). An a/b row is *pick a or b*.
A built row is *approve as built* or *change* (say what). A row with a **question** needs an answer
too. Only an approved part gets `art: final`.

In the game (`./gradlew :desktop:run --args="…"`):
- Level 08 with Rook: `--level 8 --escort rook:autocannon:2,side=right` (the traffic is still the
  placeholder kit until rows 8–9 are picked).
- The Act 1 → Act 2 transition: `--level 7 --invulnerable`. Winning it plays the act summary, the
  Act 1 outro, the Act 2 title card, the act pages, the Level 08 pages and the hangar with Rook's
  teaser.
- The intel panel: the hangar before Level 08, at each sensor level.

## Choices

| # | Part | What is proposed | Review files | Weak spots (Claude) | Outcome |
|---|---|---|---|---|---|
| 1 | [Creeper](../../enemies/ground/creeper/README.md#concept-art) | production art (`tools/art/creeper.py`, straight from the chosen round-06 model, no a/b): `creeper_0..95` (60×60, 16 headings × 6 walk frames, 22 px per gait cycle so the planted feet stay put); the 16 legless **husks**; the additive **glow** masks of the gland, the back pores and the eyes (drawn above low-air, so they show through the fog and smoke); the 16-frame `medium` **death** (violet gland burst, plus the torn legs, frill shards and chitin chips as `-tatters`). **Readability lift** after the Level 08 capture (it was dark grey-brown on the dark navy streets): chitin albedo ×1.4, legs and frill lighter, three violet glow pores along the back, a 1 px lavender-white light rim (cool, not the loot targets' warm white); the husk and death pieces in the lifted colours. The 30×30 **intel portrait** (`tools/art/intel.py`, walking down) re-rendered from the lifted model | `creeper-final-r30-a` (png, gif), `intel-final-r30-a`, `level-08-readability-r30-b` | the death pieces are posed at heading 0 only (the tatters fly the same way whatever the heading); the tatter chips were dark before the lift, so check them again; the first glow frame is flat; the weak point is drawn only (the ×1.5 is struck, a default); the walk cycle at 35 px/s on a 140 px/s scroll is best judged in the game |  **approved as final** (user): `creeper_0..95`, the husks, the glow masks, the death and tatters with the readability lift, and the intel portrait; the weak spots as they are; Creeper `art: final`, `done` |
| 2 | [Level 08 backdrop](../../campaign/act-2-homefront/level-08-neon-skyline/README.md#layout) | production art (`tools/art/backdrop_l08.py`, from the chosen `parallax-r03-b`, extended in the same kit; its block merged into the level's data). **Towers** (D1 = a): pre-rendered roofs (h 0.35–1.35; styles city a/b, burnt, neon a/b) and wall textures, projected by the game round (240, 297), never over a Creeper path or ground target. **The five districts**: the harbour (Gulf glow, quays, cranes, a container ship, yards), the avenues (lamps, parks, the landing pad), the rooftops (low roofs, gardens, the billboard's roof, the walk roofs and ramps), the smoke district (burnt blocks, fire, the collapsed overpass, the parking deck), and the Third Mainland highway with Neon Heights and the Ndidi Arcology at the end. Fog, smoke banks and columns, mist and ash on top. **Smoke parting** after the capture: the banks open over the Creepers, their husks and the ground targets at every moment; the heavy wall's cover drops from 59 % to 35 % and the medium smoke's from 27 % to 19 %. The t 130.5 plume moved off a Creeper path. The landing pad is pale, and every turret stands on a pale painted spot with a ring and lamps | `backdrop-final-r30-a` | the aircars and gunships are **placeholders** until rows 8–9; there are no searchlights (the traffic fills the motion budget); nothing is on `far`; the neon signs (SUYA, ZOBO, OKO) repeat across Neon Heights; the turret spots are a gameplay aid that a real rooftop would not have; three atlas pages for the level |  **approved as final** (user): the backdrop with its towers, the smoke parting and the pale turret spots; the placeholder traffic replaced at the close by row 9's a (`tools/art/l08_traffic.py`, `backdrop-final-r30-a` rebuilt); Level 08 `art: final` |
| 3 | [Perspective towers](../../art-direction/README.md#parallax-layer-model) | the renderer's test capture (D1 = a, built by `BackdropData.Tower`, `TowerProjection`, `BackdropCheck`): a scratch copy of Level 04's first section with a placeholder city of 114 towers (`tools/concept/towers_r30.py`). The roofs lean out from (240, 297) at *k* = 6 ÷ (6 − *h*), the walls facing the centre show true-perspective window rows, shaded right and down; units and shadows are drawn over them. Does the projection read as the parallax B city? | `towers-capture-r30-a` | a placeholder kit, not the production towers (those are in rows 2 and 11); one still frame, so the walls' turn is best seen in the Level 08 capture's video; the roof is drawn at footprint × *k* and never mirrored; the art-direction towers item is still unticked (to tick at the close) |  **accepted** (user): the projection reads as the parallax B city; the art direction's perspective-towers item ticked for Level 08 (the canyon walls stay Act 3) |
| 4 | [Act 2 title card and still](../../campaign/act-2-homefront/README.md#concept-art) | production art (`tools/art/act_stills.py act-2`, 960×540; the lettering `act-2-homefront-title.png` by `tools/concept/ui_assets.py`): Nova Lagos at night from above, built from Level 08's production backdrop (avenues, rooftops, towers projected from the still's middle, the harbour stepping down to the Gulf bottom left). Fires and smoke rise on the yards, a cloud deck over the top and right is lit from below, and eight landers (small Vrell seed pods with glowing heat shields and a faint violet halo) come down on short trails. The sheet also shows the title card over it as the game draws it ("ACT II / HOMEFRONT", darkened to 55 %, letterboxed) | `act-2-still-final-r30-a` | rebuilt once: the first version repeated the avenue at x = 480 and its landers read as meteors; re-rendered on 2026-10-07 from the backdrop after the readability pass and byte-identical: the still draws none of the pieces the pass changed (the turret nests' landing pad and roof, the parking deck, the smoke banks), and its two ringed pads, top left and top middle, are helipads on tower roofs, not the nest's landing pad; the act README says "from low orbit", the still is lower than that; the act's Concept art prose, which said it was built from the round-03 concept, is corrected |  **approved as final** (user), its lower-than-orbit view as it is |
| 5 | [Act 2 and Level 08 briefing images](../../ui/briefing/README.md#concept-art) | production art (`tools/art/briefing_images.py`, 672×240, one per page, D3). **Act 2:** `act-2-landfall` (the landers' trails through the cloud deck over the Gulf, the CDF tracking overlay), `act-2-front-lines` (the global display: three landing zones, the cities, the sea lanes, the Arctic relays), `act-2-over-home` (a Nova Lagos street from rooftop height, people looking up as Lancer passes low), `act-2-scramble` (Aegis Wing and Rook's Ember on a coastal airbase at dusk, Nova Lagos burning 40 km off). **Level 08:** `level-08-nova-lagos` (the route harbour → highways → towers → Third Mainland → the Ikoyi shelters), `level-08-walker-scan` (the Creeper, its aimed five-way fan, a convoy's fans 0.5 s apart, anti-ground ×2) | `briefing-images-final-r30-a` | `level-08-walker-scan` re-rendered on 2026-10-07 with the Creeper's lift (the other images byte-identical, the sheet rebuilt); the world map's coastlines are rough; "over home" is the least holographic of the six; the walker scan's convoy fans are busy; the numbers on the images (34 tracks, the T-numbers, 60 m, 40 km) are the agent's |  **approved as final** (user): the six images, the weak spots as they are |
| 6 | [Rook's briefing portraits and plate](../../story/characters/rook/README.md#concept-art) | production art (`tools/art/portraits.py`): his 144×144 briefing portraits in neutral, grim and fierce, his approved round-13 radio portraits at 2× with the CRT scanlines, the same treatment as Okafor's and Varga's; name plate **LT. K. TANAKA**, role line **AEGIS TWO** (his call sign, `Speaker`). Made because he speaks Level 08's hangar teaser (user decision 2026-10-06) | `rook-briefing-portrait-final-r30-a` | no screen of part B draws the 144×144 portraits: the hangar intel shows the teaser speaker's 72×72 radio portrait (decision of 2026-10-02), but `PortraitsTest` asks every teaser speaker for a briefing portrait; they are ready for a page he may speak later. For the role line see confirmation 17 |  **approved as final** (user): the three portraits, the plate LT. K. TANAKA and the role line AEGIS TWO |
| 7 | [Homefront final and base stem](../../audio/music/README.md#concept-art) | production files (`tools/art/themes.py homefront homefront-base`). Track 6 is the chosen `homefront-full-r08-a` rendered again, audio identical, with a `SOURCE` comment: 140.8 s, loop 6.94 s + 130.61 s, −14.0 LUFS, −1.6 dBTP, seam 0.21. The **base stem** comes from a new frozen generator (`music_r30.py`, the round-11 method): the same render without the lead, the brass, the counter-melody and the breakbeat top, mastered linked to the full mix: −18.2 LUFS (−4.2 LU), −3.3 dBTP, seam 0.61. Two seam aids play the last 8 s of each loop into its first 8 s. Level 08 plays the city ambience in section 1, the base stem from section 2 and the full mix from section 5 | `homefront-final-r30-a`, `homefront-base-final-r30-a`, `homefront-seam-final-r30-a`, `homefront-base-seam-final-r30-a`, `music-final-r30-a` (png) | measured only, not listened to; the base stem had no concept round (straight to production by default); its seam jump (0.61) is larger than the full mix's (0.21); the city ambience is used as it is (heard in the capture's video) |  **approved as final** (user): track 6 and its base stem (the base stem's seam and the city ambience as they are); the music track-map item ticked, music `done` |
| 8 | [Billboard](../../campaign/act-2-homefront/level-08-neon-skyline/README.md#secrets-and-pickups) | **concepts, pick a or b** (`tools/concept/props_r30.py`). The secret's ground trigger "LAGOS NEVER SLEEPS" on a low roof (t≈100): it flickers, takes 3 hits, topples and drops the CDF crate (+160). **a** "neon sky-sign": a dark panel tilted up on a steel A-frame, red neon LAGOS over cool-white NEVER SLEEPS, floodlights, an amber/black hazard catwalk; on a hit the tubes go out letter by letter; toppled face up. **b** "LED screen": a bezelled video screen with a sun hood on a hazard plinth with an olive CDF cabinet, beige pixel text on a rust screen; a glitch-band flicker, a star crack, toppled face down with the cabinet thrown open. Proposed: sprite 76×52, hit box 64×36, frames `billboard_0..2` and a cycled flicker `billboard-glow_0..7` | `billboard-r30-a`, `billboard-r30-b` (png, gif) | the cycled flicker needs a renderer addition (a trigger's `-glow` is one still frame today); the game draws a placeholder until the pick is produced; a's red neon must stay clear of the reserved hues (checked by eye only); b's text is dimmer at 1× |  **a** (user), the neon sky-sign; produced at the close (`tools/art/billboard.py`: `billboard_0..2` 76×52, `billboard-glow_0..31` looped by `GroundGlow`, review `billboard-final-r30-a`), its topple a destructible's break (small explosion, small blast, large crumble: `TRIGGER_SPENT`, `TriggerBreak`); b moved to `concept/rejected/` |
| 9 | [Traffic](../../campaign/act-2-homefront/level-08-neon-skyline/README.md#layout) | **concepts, pick a or b** (`tools/concept/props_r30.py`). Low-air scenery (D2 = a: no collision, shots pass through), muted below the play plane (82 % brightness, 75 % saturation, no rim, no loot amber, no enemy hues), lit by headlights, tail lights, nav lights and a strobe. **a** "wedge cars": a wedge sedan 12×20 and a box van 14×24 on corner lift ducts, a CDF gunship 32×40 with twin ducted fans. **b** "pods": a teardrop sedan 12×20, an evacuee minibus 14×26 with a lit window band, a CDF armoured hauler 30×48 on four tilt ducts. Proposed: 16 headings, streams by `repeat` (a car about every 0.37 s per southbound lane), 300 px/s south and 80 px/s north on the screen | `traffic-r30-a`, `traffic-r30-b` (png, gif) | the cars are 12–14 px wide, so at 1× they read mostly by their lights; a stream under the ship's fire must never look like a target (the sheets' 2× crop shows the shots passing over); the gunship/hauler pair is the brightest craft on screen |  **a** (user), the wedge cars and the CDF gunship; produced at the close (`tools/art/l08_traffic.py` through `backdrop_l08.py`: sedan 24², van 28², gunship 44², 16 headings, one paint per piece; review `traffic-final-r30-a`); b moved to `concept/rejected/` |
| 10 | [Civilian voice](../../audio/voice/README.md) | **audition, pick a or b** (`tools/concept/audio/tts_r30.py`, Chatterbox, radio filter b, neutral): the Ikoyi shelter civilian ("shelter nine"), both lines 1 s apart. **a** KirksVoice (LibriVox, public domain, clip at 111 Hz); **b** Faith Abiola-Ellison (LibriVox, public domain, 151 Hz). Until the pick his speaker row is `uncast` and his two lines play as text | `voice-civilian-r30-a`, `voice-civilian-r30-b` (clips `ref-civilian-r30-a`, `-b`) | re-rendered on 2026-10-07 with the call's current wording ("Shelter nine, Ikoyi! Walkers on the roofs, heading our way!"; the first takes read the earlier "Aegis, this is shelter nine in Ikoyi! They're on the roofs above us!"): a 10.76 s, b 10.84 s; Whisper reads a back word for word and hears b's "shelter nine" as "Sheltonine" twice; b is close to Okafor's pitch (150 Hz; b's takes 149 / 148 Hz), and Okafor answers him at t=120.5; the README's prose says "his", so change it if b; measured only, not listened to |  **b** (user), Faith Abiola-Ellison (the README now says "her"): her clip renamed `refs/ref-civilian.wav`, her two lines voiced (both takes pinned), `AUDITIONING` emptied; a moved to `concept/rejected/`, its clip deleted with its CREDITS.md row |
| 11 | [Level 08 in the game](../../campaign/act-2-homefront/level-08-neon-skyline/README.md#concept-art) | game captures. **Level 08** at medium with Rook (autocannon L2), the balance plan's fit plus a bomb rack, flown by a small driver: the harbour, Rook's side and first-kill lines, the towers in each district, a convoy's fans and deaths, the three turret nests, the overpass mortars, the billboard and its crate, the smoke district, the Choir at t 165 and Varga's reply, the arcology, the debrief (153 / 189 kills, every Creeper stopped, A+, 2,116 credits). **The transition** from Level 07's win: the act summary, the outro, the Act 2 title card, the briefing pages, the hangar with Rook's teaser, the launch. **The readability strip**: the same level times before and after the readability pass (rows 1 and 2), each with its silhouette contrast | `level-08-capture-final-r30-a` (png, [mp4](../../campaign/act-2-homefront/level-08-neon-skyline/concept/level-08-capture-final-r30-a.mp4)), `level-08-readability-r30-b` | the main capture **predates** these fixes: the readability pass, the Creeper's lift, the shortened Level 08 pages (it shows eight briefing screens, now six), the teaser's role line, the outro's NEXT panel, and Rook's corner lock below the ship after t≈141 (the readability strip, `-r30-b`, shows the new look of the level; the other fixes are not captured); the briefing screens were advanced every 11.7 s, which cuts the longer voices; two panels come from an earlier run; the traffic is the placeholder kit; the mix touches full scale (53 samples, none over) |  **accepted** (user): the captures as taken, the fixes after them as built |
| 12 | [Level 08 and Act 2 lines voiced](../../audio/voice/README.md) | **listening row**: 19 new lines rendered with the cast voices (`tools/art/voice.py`). That is the Act 2 act briefing's four pages and Level 08's two (dry), ten radio lines with Rook's `{side}` line once per side, his `escort-first-kill` line, and the billboard secret's line (Okafor 10, Varga 4, Rook 5). Quoted under [Listening](#listening). Accept as rendered, or name the lines to re-render | the files under [Listening](#listening) | listen to Level 08 briefing page 1 for **"Bring each other home"**, **"Ikoyi"** and **"Vrell"**: Whisper reads them back only with a name prompt, on every seed tried; three Okafor takes are pinned: act page 3 ("you've fought"), t=1 ("contact inbound") and the level end ("all colleges" for "arcologies"); the hangar teaser is not voiced, by design; reviewed by Whisper only |  **accepted as rendered** (user): all 19 lines, the three pinned Okafor takes included; none to re-render |
| 13 | [Texts for review](#texts-for-review) | **text review** (D3 = a; Level 08's document is `design: review`). The four Act 2 act briefing pages, Level 08's two briefing pages (shortened to fit one screen each), Rook's hangar teaser, Varga's four intel lines and the radio script, all quoted under [Texts for review](#texts-for-review). Approve, or say what to change | — | changing a voiced text means a re-render (row 12); the Act 1 outro's last page already tells Rook's assignment, so the Level 08 pages mention him only as "First sortie with Rook on your wing"; "forty million" and "since dawn" are new facts about Nova Lagos |  **approved** (user): every text as quoted; Level 08's and Act 2's documents leave `review` for `approved` |
| 14 | [Part B numbers](#part-b-numbers) | accept or change the numbers listed under [Part B numbers](#part-b-numbers): density, pacing, the credit budget, the Creeper's numbers, the radio retimes and the autopilot's runs | — | the autopilot's per-difficulty kills and credits were not written down, only its grade (A+ on all three); the capture run's 2,116 credits include the grade bonus |  **accepted** (user) |
| 15 | [Part B decisions](#user-decisions-of-part-b-check-they-were-carried-out) | check that the user decisions D1–D6 and the stated defaults listed under [Part B numbers and texts](#user-decisions-of-part-b-check-they-were-carried-out) were carried out | — | D2 and D6 are carried out as far as part B goes before this round: the traffic's and the billboard's production waits for rows 8 and 9 |  **checked** (user): every decision carried out; the art direction's towers item ticked at the close |
| 16 | [One screen per page from Act 2 on](../../ui/briefing/README.md#decisions) | **confirm**: a new rule found in the capture. From Act 2 on, an act page and a level page each fit one screen (4 lines below the image; `BriefingLayoutTest.anActPageAndALevelPageFromAct2OnFitOneScreen`). Act 1's level pages were written and voiced to run on over the next screens and keep doing so, as the act outros do. Keep this rule? | `level-08-capture-final-r30-a` (the long pages before the fix) | the rule limits a page to four lines of text, so later acts' briefings say less per page or use more pages; Act 1 keeps its longer pages, so the two acts read differently |  **kept** (user): from Act 2 on, an act page and a level page each fit one screen (recorded in the [briefing decisions](../../ui/briefing/README.md#decisions)) |
| 17 | [Teaser speaker's role line](../../ui/hangar/README.md#decisions) | **confirm**: the hangar intel's line under the teaser speaker's name now comes from the speaker's role (`Speaker.role`): `AEGIS TWO` for Rook (it read `LT. K. TANAKA / CDF INTEL` in the capture), `CDF INTELLIGENCE` for Varga and `CDF COMMAND` for Okafor (Level 04's teaser). The user's decision of 2026-10-02 named a fixed `CDF INTEL`. **Question:** keep the role labels, or restore `CDF INTEL` for Varga (and keep the role only for the others)? | — (seen in the hangar before Levels 01–08) | `CDF INTELLIGENCE` is 16 characters against 9; `IntelPanelLayoutTest` passes for every level at every sensor level |  **question answered** (user, asked separately): **keep the role labels** (`Speaker.role`: Varga `CDF INTELLIGENCE`, Okafor `CDF COMMAND`, Rook `AEGIS TWO`); this supersedes the 2026-10-02 fixed `CDF INTEL` (recorded in the [hangar decisions](../../ui/hangar/README.md#decisions)) |

## Part B numbers and texts

The numbers and texts the agents chose or measured to build part B (rows 13–17). The details are in
the parts' READMEs (Decisions of 2026-10-06 and 2026-10-07). The design source is the gap analysis
of part B (D1–D6 of 2026-10-06).

### Texts for review

**Act 2 title card** ([Act 2](../../campaign/act-2-homefront/README.md#act-intro-and-outro)): ACT II
· HOMEFRONT · Earth · May 2185.

**Act 2 act briefing**, four pages by Commander Okafor, each with its image:

> 1 (grim, `act-2-landfall`): "Twelve days. That is how long we watched the second fleet come in.
> We tracked every ship of it, and we could not stop one. At four o'clock this morning it came down
> through the cloud deck in three places: the Gulf of Guinea, the Java Sea and the River Plate."
>
> 2 (`act-2-front-lines`): "The landers came down on the cities. The army holds the streets where
> it can; the sky is ours to hold. Aegis Wing goes where the line is breaking: the cities first,
> then the sea lanes, then the Arctic relays that tie the planetary defence grid together."
>
> 3 (`act-2-over-home`): "Until now you fought over shipyards and craters, in vacuum. From today
> you fly over home: over streets you know, and over people who will be watching the sky for you.
> Fly accordingly."
>
> 4 (fierce, `act-2-scramble`): "We could not stop them landing. We can stop them staying. Mission
> briefing follows. Aegis Actual out."

**Level 08 briefing**, two pages, shortened on 2026-10-07 to fit one screen each
([Level 08](../../campaign/act-2-homefront/level-08-neon-skyline/README.md#briefing)):

> **Commander Okafor** (`level-08-nova-lagos`): "Nova Lagos has fought since dawn, Lancer: forty
> million people, and the Vrell among them. Fly low: harbour, highways, towers, then the Third
> Mainland to the Ikoyi shelters. First sortie with Rook on your wing. Bring each other home."
>
> **Dr. Varga** (`level-08-walker-scan`): "The walkers are new. We call them Creepers. They crawl
> the streets and low roofs and fan their fire at you every few seconds. Anything that hits the
> ground hurts them twice as much. Every one you stop never reaches the shelters."

**Hangar teaser** (Rook, `LT. K. TANAKA / AEGIS TWO`, not voiced): "Escort slot's mine now, Lancer.
Fit me a gun, and bring something that hits the ground."

**Varga's intel lines** (one per sensor-suite level, `IntelPanelLayoutTest`):

| Sensor suite | Line |
|---|---|
| none | "Nova Lagos at night, Lancer. Walkers in the streets, flyers over the towers. Lots of both." |
| L1 | "Nearly all from ahead, two pincers from the flanks, nothing behind you. Smoke late on, but it stays below you." |
| L2 | "New one: the Creeper, a walker. It fans five shots at you every three seconds. Turrets and mortars sit on the low roofs." |
| L3 | "Anti-ground hits Creepers twice as hard. One cache: under the flickering billboard. Three hits bring it down." |

**Radio script** ([Level 08](../../campaign/act-2-homefront/level-08-neon-skyline/README.md#radio-chatter)):

| Trigger | Speaker | Line |
|---|---|---|
| t=1 | Okafor | "Aegis, contacts inbound over the harbour. Weapons free." |
| t=8.5 | Rook | "Copy. Lancer, I'm on your {side}. Don't make me look bad." (*left* / *right*, by his side) |
| Rook's first kill (`escort-first-kill`) | Rook | "Splash one. I'm keeping count, by the way." |
| t=26 | Okafor | "The traffic below you is civilians getting out. Keep firing." |
| t=43 | Rook | "Turrets on that landing pad. Mind the thorns." |
| t=66 | Varga | "Walker on the low roofs. It sprays wide. Hit it hard." |
| t=84 | Varga | "Bombs and mortars hit those walkers twice as hard." |
| t=113 | Civilian (shelter nine) | "Shelter nine, Ikoyi! Walkers on the roofs, heading our way!" (text until cast, row 10) |
| t=120.5 | Okafor | "We hear you, shelter nine. Lancer, clear those roofs." |
| t=157 | Okafor | "Convoy on the Third Mainland. Keep it off the shelters." |
| t=165 | The Choir | "[the Choir sings]" (its round-29 stage sound) |
| t=171 | Varga | "That's the song from orbit. It's louder down here." |
| Billboard cache | Rook | "Billboard's down, and there's a CDF crate under it. Grab it." |
| Level end | Okafor | "Good flying, both of you. Get some rest. The arcologies are next." |
| Secondary met | Civilian (shelter nine) | "Shelter nine here. The roofs are quiet. Thank you, Aegis." (text until cast) |

The HUD prompt at t=6 for 6 s reads `ROOK SIDE` · `IN THE HANGAR` (`ROOK'S SIDE` is 6 px too wide for
the prompt line). The secondary objective shows as "BONUS: NO CREEPER GETS THROUGH".

### Part B numbers

From [Level 08](../../campaign/act-2-homefront/level-08-neon-skyline/README.md) and its data:

- **Length:** 140 px/s, about 200 s (28,000 px), launch 5 s; the opener is about 10 s (D5 = a),
  the first Skitters at t=12.
- **Density:** **189 enemies at medium** (air 162, ground 27: 15 Creepers, 9 Spine Turrets, 3
  Polyp Mortars), **58.2 a minute** over the 195 s after the launch (Act 2's minimum is 50).
  Waves: Skitter 108, Needler 42, Stinger 12, Creeper 15. Per section: 1 none, 2 60, 3 52, 4 50,
  5 27. The design had 181; the data step added two Skitter streams (t=110.5, t=159.5).
- **Pacing:** the screen is empty for more than 3 s only twice: the opener (about 7 s) and the
  quiet end over the arcology (about 183–200 s). Eleven waves were moved up by 1–2.5 s to get there
  on every difficulty.
- **Difficulty:** Creepers 14 / 15 / 17 (easy / medium / hard). Easy: the two Needler pincers come
  from the front, the t=84 convoy has 2, and there is an extra armour patch at t≈130. Hard: one more
  Creeper in the t=84 and t=162 convoys, the 7-way fan every 2.6 s, the Needler circle breaking in
  pairs, and two Spine Turrets at the overpass.
- **Credit budget:** budget(8) = 700 × 1.07⁷ ≈ **1,124**. **`bounty_scale` 0.56**: the typical haul
  is **1,105** (−1.7 %) and a perfect run **1,738** (1.55 × budget). With 0.57 the Skitter's bounty
  rounds up to 5 and the typical haul lands at +5.0 %. The bounties paid at Level 08 (× 1.6 × 0.56,
  rounded per kill): Skitter 4, Needler 11, Stinger 13, Creeper 20, Spine Turret 11, Polyp Mortar
  13. The data keeps Act 1 terms: the billboard crate is 100 (pays 160) and the secondary 56 (pays
  90). Rook's kills pay like the player's and do not move the typical haul.
- **Radio retimes** (the 1 s rule): t=1/3 → 1/8.5, t=112/113 → 113/120.5, t=166/167 → 165/171 (the
  Choir's 3.9 s sound and 5.6 s subtitle). Rook's first kill has its gaps at 15.8–26 s and 33.4–43 s,
  and the billboard's line at 91–113 s. `RadioTimelineTest` flies the level with Rook on both
  sides, with his barks and the first-kill event; no voiced timed line starts more than 1 s late.
- **Autopilot** (`Level08Test.theAutopilotFliesTheLevelToItsEndWithRook`: the balance plan's fit
  with Rook on its wing): it flies the level to its end with grade **A+** on easy, medium and hard,
  and Rook's first-kill line plays at most once an attempt. The capture's run (row 11, invulnerable,
  flown by hand) made 153 / 189 kills, stopped every Creeper and got A+ with 2,116 credits.

**The Creeper** ([stat block](../../enemies/ground/creeper/README.md#stat-block), first level 08):

| Field | Value |
|---|---|
| HP | 30 (easy 22, hard 39); time to kill about 0.5 s at Level 08's reference DPS 60 |
| Size | 60×60, hit box 40×44, `medium`, `ground` |
| Walk | 35 px/s on its path, turning 90°/s, 22 px per gait cycle |
| Convoy | 3–5 on one path, 1.5 s apart (about 160 px on screen at 105 px/s down the screen) |
| Fan | 5 bullets aimed at the player, 50° spread, 140 px/s, `small` (4), every 3 s; the first volley half an interval after the first unit is on screen |
| Stagger | unit *i* fires *i* × 0.5 s after the volley's start (a shared clock per wave); five units take 2.0 s of the 3.0 s |
| Hard | 7-way every 2.6 s, an authored interval (no fire-rate lever on top) |
| Weak point | the violet fan gland, drawn only (×1.5 struck) |
| Traits | `anti-ground` (×2), `area` |
| Bounty | 22 (pays 20 at Level 08), score 220 × chain |
| Death | the `medium` organic burst, a husk at its last heading; the Vrell screech as it first comes onto the screen |

### User decisions of part B (check they were carried out)

- [x] **D1 = a**: the towers in true perspective are scenery only (`BackdropData.Tower`,
  `TowerProjection`). Turret nests, Creepers, the billboard and its crate sit at street level or on
  low structures drawn without lean, and no Creeper crawls a wall (rows 2 and 3).
- [x] **D2 = a**: the traffic is scenery on `low-air` (the sim never sees it, shots pass through).
  It flies as placeholders until row 9 is picked.
- [x] **D3 = a**: the intro texts are rewritten. Act 2 has four pages, Level 08 two, each with its
  image, with no repeat of the Act 1 outro; "forty hours ago" against "overnight" is resolved (the
  landfall at four this morning), and the t=1 line no longer assigns Rook (row 13).
- [x] **D4 = a**: the `escort-first-kill` event (his kills only, once an attempt, never after an
  eject) and the `{side}` line with two voiced takes (rows 12 and 13).
- [x] **D5 = a**: the opener is about 10 s, the level is densified with its own units (189 at medium),
  and the Creepers went from 12 to 15; the secondary now asks for every Creeper.
- [x] **D6 = a**: the chosen concepts and the four new backdrop sections went straight to production
  (rows 1, 2, 4–7). The billboard, the traffic and the civilian voice are a/b (rows 8–10); their
  production follows this round.
- [x] **Defaults:**
  - the data in Act 1 terms (crate 100, secondary 56);
  - no act HP factor on Level 08's units (all `tiny` or `small`);
  - Rook's scripted t=92 flank line dropped (the pincers set off his bark);
  - Rook launches in formation (no glide-in);
  - his side prompt at t=6;
  - the radio voiced and retimed;
  - the Creeper's weak point drawn only, its aimed fan, the 0.5 s stagger, the 16-heading husk and
    the screech;
  - the ambience in section 1, the base stem from section 2, the full mix from section 5;
  - the title card over its still, with one image per act page and two for Level 08;
  - an M4 save already at the Level 08 hangar skips the intro (`--level 7` shows it);
  - Act 2 keeps the Act 1 tactical map;
  - the campaign README's `rear` row ticked.
- [x] The art direction's perspective-towers item, ticked for Level 08 at the close (the canyons
  stay Act 3).

## Listening

The 19 new voiced lines (row 12) as the game plays them, from `assets/voice/`. The files were matched
to their lines by the voice key, the file name (`VoiceLines.key`, as in the render log of
`tools/art/voice.py`). The civilian audition (row 10) is on the board's cards. His two lines play
as text until he is cast.

| Line | Speaker | Text | Take |
|---|---|---|---|
| Act 2 page 1 (grim) | Okafor | "Twelve days. That is how long we watched the second fleet come in…" (19.9 s) | [play](../../../assets/voice/okafor/08505ce3f768.ogg) |
| Act 2 page 2 | Okafor | "The landers came down on the cities…" (19.1 s) | [play](../../../assets/voice/okafor/330285ba59a8.ogg) |
| Act 2 page 3 | Okafor | "Until now you fought over shipyards and craters, in vacuum…" (pinned take, 15.5 s) | [play](../../../assets/voice/okafor/468143d57448.ogg) |
| Act 2 page 4 (fierce) | Okafor | "We could not stop them landing. We can stop them staying…" (8.8 s) | [play](../../../assets/voice/okafor/a918a55eda70.ogg) |
| Briefing page 1 | Okafor | "Nova Lagos has fought since dawn, Lancer…" (listen for "Vrell", "Ikoyi" and "Bring each other home", 20.2 s) | [play](../../../assets/voice/okafor/7ba28277d1df.ogg) |
| Briefing page 2 | Varga | "The walkers are new. We call them Creepers…" (12.9 s) | [play](../../../assets/voice/varga/b7a109eab571.ogg) |
| t=1 | Okafor | "Aegis, contacts inbound over the harbour. Weapons free." (pinned take) | [play](../../../assets/voice/okafor/d16b2f349c6c.ogg) |
| t=8.5, left | Rook | "Copy. Lancer, I'm on your left. Don't make me look bad." | [play](../../../assets/voice/rook/8e3ce9c411ff.ogg) |
| t=8.5, right | Rook | "Copy. Lancer, I'm on your right. Don't make me look bad." | [play](../../../assets/voice/rook/6f179cf6af36.ogg) |
| Rook's first kill | Rook | "Splash one. I'm keeping count, by the way." | [play](../../../assets/voice/rook/9e93e86a44d8.ogg) |
| t=26 | Okafor | "The traffic below you is civilians getting out. Keep firing." | [play](../../../assets/voice/okafor/866bab0aa00d.ogg) |
| t=43 | Rook | "Turrets on that landing pad. Mind the thorns." | [play](../../../assets/voice/rook/eda4414b9e2f.ogg) |
| t=66 | Varga | "Walker on the low roofs. It sprays wide. Hit it hard." | [play](../../../assets/voice/varga/b16e539632ae.ogg) |
| t=84 | Varga | "Bombs and mortars hit those walkers twice as hard." | [play](../../../assets/voice/varga/02beeec27d5d.ogg) |
| t=120.5 | Okafor | "We hear you, shelter nine. Lancer, clear those roofs." | [play](../../../assets/voice/okafor/10e598d32390.ogg) |
| t=157 | Okafor | "Convoy on the Third Mainland. Keep it off the shelters." | [play](../../../assets/voice/okafor/6c94c47bc857.ogg) |
| t=165 | The Choir | "[the Choir sings]" (round 29's stage sound, for the t=171 reply) | [play](../../../assets/voice/choir/voice-choir-sings-r29-b.ogg) |
| t=171 | Varga | "That's the song from orbit. It's louder down here." | [play](../../../assets/voice/varga/3744caacb298.ogg) |
| Billboard cache | Rook | "Billboard's down, and there's a CDF crate under it. Grab it." | [play](../../../assets/voice/rook/ce8fbe244d73.ogg) |
| Level end | Okafor | "Good flying, both of you. Get some rest. The arcologies are next." (pinned take) | [play](../../../assets/voice/okafor/e2da9c95df0f.ogg) |

## Notes

- Review file names: `<subject>-final-r30-a.png/.gif/.ogg` (production art and audio),
  `<subject>-capture-r30-a` / `level-08-capture-final-r30-a` / `level-08-readability-r30-b` (game
  captures), and `<subject>-r30-a/b` (the three a/b picks), in each part's `concept/`. Their
  `prompts.md` entries say how they are made. Nothing in `assets/` is hand-edited.
- The board shows the video `level-08-capture-final-r30-a.mp4` as a link ("open").
- The Creeper's intel portrait was re-rendered on 2026-10-07 after its lift (`python3
  tools/art/intel.py creeper`): only `assets/sprites/intel/creeper.png` and
  `design/ui/hangar/concept/intel-final-r30-a.png` changed; every other intel picture is byte-identical.
  `IntelPanelLayoutTest` passes.
- Refreshed on 2026-10-07 after the last fixes: the civilian audition (row 10) with the call's
  current wording (`tts_r30.py`); `level-08-walker-scan` with the lifted Creeper (`python3
  tools/art/briefing_images.py`: only that image and the r30 sheet changed, the other 28 images are
  byte-identical); the Act 2 still (`python3 tools/art/act_stills.py`) re-rendered byte-identical,
  Act 1's too (row 4). `BriefingLayoutTest` and `VoiceFilesTest` pass.
- The captures were taken with `--invulnerable` on a private display; their prompts entries list
  the exact options.
- After the choices (at the close):
  - the approved parts get `art: final`;
  - the picked billboard and traffic go to production (`tools/art/`, sizes into Level 08's data, the
    cycled flicker in the renderer), and the rejected ones move to `concept/rejected/`;
  - the cast civilian voice renders his two lines (`uncast` dropped, `AUDITIONING` emptied);
  - Level 08's design leaves `review` once the texts are approved.

## Decisions

- 2026-10-07: Opened with M5 part B.
- 2026-10-07: Closed (user): **everything accepted**, with the billboard **a** (the neon sky-sign;
  b, the LED screen, rejected), the traffic **a** (the wedge cars and the CDF gunship; b, the pods,
  rejected) and the Ikoyi shelter civilian **b** (Faith Abiola-Ellison; a, KirksVoice, rejected).
  The Creeper, Level 08's backdrop and towers, the Act 2 still, the six briefing images, Rook's
  briefing portraits and "Homefront" with its base stem approved as final; the captures, the 19
  voiced lines, the texts, the part B numbers and the D1–D6 checklist accepted, so Level 08's and
  Act 2's documents are `approved`. Row 16's rule (one screen per page from Act 2 on) kept; row 17
  answered separately: keep the role labels (`Speaker.role`), superseding the 2026-10-02
  `CDF INTEL`. The billboard and the traffic were produced at the close (`tools/art/billboard.py`,
  `tools/art/l08_traffic.py`), the civilian's lines voiced. The rejected files are in
  `design/campaign/act-2-homefront/level-08-neon-skyline/concept/rejected/` and
  `design/audio/voice/concept/rejected/`; the rejected reference clip is deleted with its
  CREDITS.md row. M5 part B is done; part C (Level 09, round 31) is next.
