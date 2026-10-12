---
title: Concept round 34 — M5 parts F to I, Levels 12 to 14, the Act 2 end and the close-out
design: review
implementation: n/a
art: proposed
depends-on: [../../campaign/act-2-homefront, ../../campaign/act-2-homefront/level-12-storm-front, ../../campaign/act-2-homefront/level-13-polar-relay, ../../campaign/act-2-homefront/level-14-siege-spire, ../../enemies/air/lamprey, ../../enemies/naval/skimmer, ../../enemies/bosses/siege-spire, ../../allies, ../../ui/briefing, ../../ui/hangar, ../../ui/hud, ../../audio/sfx, ../../audio/voice, ../../audio/music]
updated: 2026-10-12
---

# Concept round 34 — M5 parts F to I, Levels 12 to 14, the Act 2 end and the close-out

## Summary

The combined review round of M5's last four parts ([roadmap](../../tech/roadmap/README.md#m5-parts)),
built while the user was on vacation ("collect everything for one large round at the end"). It
replaces the planned rounds 34 (part F, Level 12 *Storm Front*), 35 (part G, Level 13 *Polar Relay*),
36 (part H, Level 14 *Siege Spire* and the Act 2 end) and 37 (part I, the close-out); the concept
files keep their `r34`, `r35` and `r36` names. Every pick, decision and build choice below was taken
on the user's behalf (the recommendation, variant **a**) and is **pending the user's review**.

Open [index.html](index.html) in a browser (regenerate with `python3 tools/concept/board.py 34 35 36`).
The board shows every `r34`/`r35`/`r36` file by part; this README is the list of choices, one section
per level and one for part I, each in the same order:

1. **a/b picks**: variant a is built into the game, b exists as review files: *pick a or b*;
2. **final-art reviews** (production art straight from the chosen concepts): *approve as final* or
   *redo* (say what);
3. **captures** of the game: *accept* or *change*;
4. **sounds** (each proposal *accept* or *redo*; a/b sounds *pick*) and **voices** (*listen*; the
   lines worth a listen are marked LISTEN in [Listening](#listening));
5. **texts**, quoted in full: *accept* or *change*;
6. **numbers**: *accept* or *change*;
7. **decisions** taken on the user's behalf (*check*) and **build choices** (each *yes* = keep as
   built, or *no* = say what to change).

Read the two boxes first: what changed in **approved** specs, and the problems still open.

## Changes to approved specs

Approved parts of the design (the Acts 1–2 review of 2026-10-01 and the user's later rules) that the
build changed. Each **C** row is *accept* or *revert*; checked against the approved text in git
(`5d8204c`) and the docs as they are now.

| # | Change | Approved | Built (where) |
|---|---|---|---|
| C1 | **The Spire fight's creep is dropped** (H1 = a) | Level 14: "During boss phase 1 the scroll creeps at 15 px/s, so the roots come into range in turn" | the arena is a halt (speed 0), all six roots on the screen from the start; "in turn" comes from the staggered fire and the player's choice of root ([Level 14](../../campaign/act-2-homefront/level-14-siege-spire/README.md#decisions)) |
| C2 | **Level 14's root field scrolls at 80 px/s** | section 3 *Root Field* at 140 px/s | 80 px/s, so a growth (1 s after its dust, firing 0.75 s later) is not 350 px down the screen before it fires (step H4) |
| C3 | **Level 14's daylight drifts at 25 px/s** | section 6 *Daylight* 253–265 at 140 px/s | 12 s at 25 px/s (40 after H7b, 25 after H9) so the stump stays on the screen while the light falls on it; 13,320 px of ground in all |
| C4 | **Level 13's grid online and jingle times** | section 5 *Grid Online* 220–235 (the relay's lights); the mission-complete jingle "on *Grid sync complete*" (t=218); Okafor's "Well held" at t=220 | the lights and the grid-online sound at t=218 (the sync's end); the theme fades with the 2.6 s chord and the jingle starts at **t=220.6** (`jingle: {t: 218, fade: 2.6}`, step G8); "Well held" at t=233.5; the debrief silent |
| C5 | **Shortened and split briefing texts, new teasers and intel lines** | one Okafor page each for Levels 13 and 14, a longer Level 12 page 1, one teaser and one Varga intel line per level | two one-screen pages per level (Okafor, Varga), shortened; Level 12's and 14's teasers rewritten for Rook; four intel lines per level. Both versions are quoted under each level's *Texts* |
| C6 | **Level 14's hard fight runs over the window** | H10: a fight of ≈ 135 s inside 90–180 s | hard: `SiegeSpireTest` mean **195 s** (4 of 5 won), full level runs **204 s**; medium 129.5 s, easy 95.8 s |
| C7 | **The maw's launch lanes are spread evenly** | the Siege Spire spec: a launched Wraith "swoops down a lane toward the player's side" (the spec still says so) | the lanes lie evenly across the field as a rear ambush wave's: x 160 / 320 for two, 120 / 240 / 360 for three (`sim/.../Launches.java`, step H3) |
| C8 | **The Spire's bounty is 26.8 % of the budget** | 720 ≈ 30 % of the old L14 budget; H10: ≈ 506 paid (30 % of budget(14) 1,687) | 372 in Act 1 terms, **452 paid = 26.8 %**: at 30 % a perfect run falls under the economy's 1.4 × budget (`TypicalHaulTest`) |
| C9 | **The Spire's HP ×1.8** (H10 = a, measured) | 6,690 in all (pods 370, mortar roots 430, maw 1,600, core 2,750) | 12,040 (665 / 775 / 2,880 / 4,950): with the spec's HP the autopilot's fight lasted ≈ 76 s |
| C10 | **The HUD's radio rule, for all levels** ([HUD](../../ui/hud/README.md#decisions), back to `design: review`) | 2026-10-02/03 (user): timed lines first; an event line waits for a gap and is stale after 6 s; closing lines never dropped | H7b: a `boss-phase` line is stale after **20 s** and dropped at the boss's death; a secondary line still waiting at a boss warning is **dropped** (or cut under 1.5 s). H9: a closing line queued on a **free** radio goes **ahead** of the next timed line, which then may start more than 1 s late. Effect: Level 14's root-grid line plays at ≈ 93.5, the Choir at ≈ 104 and Varga's "that's the Spire" at ≈ 110, **over the Spire's arrival** (t=108) |
| C11 | **Level 13's relay integrity** (G2 = a, measured) | 600 medium / 900 easy / 450 hard | **240 / 340 / 520**: hard above medium because hard's load is 3.4 × medium's |
| C12 | **Level 14's timeline and radio** (stated defaults) | the boss warning before an arrival at t=113; "no enemies before t=24"; Hammer Lead t=16, Rook t=7.5 and t=80, Varga t=22, the Choir t=98 | warning, arrival and checkpoint all at **t=108**; popcorn from t=8; Hammer Lead t=13, Rook t=21 (t=80 dropped: his bark says it), Varga t=31.5, the Choir t=96 |
| C13 | **The Act 2 end order** (H6 = a; [Act 2](../../campaign/act-2-homefront/README.md), back to `design: review`) | "the act complete fanfare plays over L14's daylight outro, followed by the act debrief" | Act 1's order: the kill → the daylight without music → the debrief with `ACT II COMPLETE · HOMEFRONT` (the mission-complete jingle) → four voiced outro pages with *act complete* under page 1 → the hangar (`BEFORE L15 - NOT BUILT YET`) |

## Open problems

Not decided by the build; each **P** row needs the user's call (or is a known gap).

| # | Problem | What we know | Proposal (pending) |
|---|---|---|---|
| P1 | **Rook ejects** | `ActPlaythroughTest` (Acts 1–2 as one campaign, Rook from L08 with repairs): he ejects in Level 14's Spire fight on **medium and hard**, and on **hard** in Levels 10 and 11 too | accept (he comes back repaired), or make the Spire / hard kinder to him |
| P2 | **The capture pilot cannot survive the Spire's phase 1** | the H8 run on the balance plan's L14 fit (cores included, no `--invulnerable`) was lost five times, 28–37 s into the fight, no kill in 420 s. H8b calibration (armour lost per 10 s): the scripted driver 31–40 at the Spire against 9–11 at the Kraken and 4–10 at the Brood Carrier (it loses there too); the autopilot 1.6 / 1.5 / 0.8 and wins all 8. The Spire's damage to the driver: pods' aimed orbs 54 %, the lob's ring 36 %, its direct hit 9 % (both track the ship) | no data change (the driver does not sidestep lob markers); **design call**: a kinder Spire for non-dodgers (e.g. two pods firing straight down)? |
| P3 | **Act 2 ends far richer than the static sheet** | `ActPlaythroughTest`: every level won at the first attempt on every difficulty; Act 2 ends with **18,251 / 14,669 / 10,897** credits (easy / medium / hard) | review the economy (the typical haul per level is on budget; the surplus is the autopilot's perfect flying) |
| P4 | **Level 12's Wraith rear check** | the plan's rear gun against the returning Wraith (18 HP with the act HP factor): **0.62** of its window, over the limit 0.6 (`BalanceTest.REAR_PENDING`, printed by `balance.py` as an accepted exception) | accept, or lower the Wraith's L12 HP / raise the plan's rear gun |
| P5 | **Not captured** | still unseen: Level 13's relay-lost mission-failed screen; Level 12's Spore Bomber revealed by a flash (no Spore Bomber is under a bank at a flash in the run). Captured since (step CF, 2026-10-12): Level 14's daylight at 25 px/s (14.3d: the stump on the screen ≈ 21 s after the kill), Level 13 after G5c (13.3c: every hull on water) and the Proximity Mines in play (12.3c: the arm chirp is in the mix, **the drop is not heard**) | capture the two left when the user wants (one short run each); the mine drop's level is a call for 12.3c |
| P6 | **Easy Level 14's Airstrike charge refused** | the plan buys a charge the special already carries at its most (4) | accepted in the test as "the special carries its most" |
| P7 | **The `sub` layer's three-OS check** | needs CI on Windows and macOS | after the user commits and pushes |
| P8 | **Human item: the voice listen-through** | 53 new voiced lines (below) plus Nansen's audition; none listened to by Claude (Whisper and pitch only) | the user listens ([Listening](#listening)) |
| P9 | **Human item: the user's playthrough of Act 2** | Levels 08–14 and the act end | the user plays (`--level 8`, or from a save after Level 07) |

## In the game

`./gradlew :desktop:run --args="…"` (add `--settings <file>` to keep the real settings apart; `--difficulty easy|hard`):
- Level 12 with the capture's fit: `--level 12 --escort rook:autocannon:1 --loadout front=pulse-cannon:4,left=micro-missile-pod:2,right=micro-missile-pod:2,rear=tail-gun:1 --special airstrike:3`
  (no `--invulnerable`: it keeps the Lampreys from latching). Eye a, latch a and round 34's a sounds are in the game.
- Level 13: `--level 13 --escort rook:autocannon:1` (the plan's fit; relay a, whiteout a and round 35's a sounds;
  Nansen's lines play as text until he is cast).
- Level 14 and the act end: `--level 14 --escort rook:autocannon:1 --loadout front=lance-laser:5,left=micro-missile-pod:5,right=micro-missile-pod:5,rear=tail-gun:2 --special airstrike:3`
  (add `--invulnerable` to see the Spire die; the act end follows a real kill). Root burst a, daylight a and round 36's a sounds are in the game.
- The way in: `--level 11 --invulnerable` (or 12, 13) plays the debrief, the next level's briefing pages and the
  hangar with Rook's teaser and the intel panel.

## Level 12 — Storm Front (part F, files r34)

Level 12 is `design: review`; [its document](../../campaign/act-2-homefront/level-12-storm-front/README.md),
the [Lamprey](../../enemies/air/lamprey/README.md). Part F's questions F1–F9 were **the user's own
answers** (2026-10-09); only the build after that is pending.

### Choices — Level 12

| # | Part | What is proposed | Review files | Weak spots (Claude) | Outcome |
|---|---|---|---|---|---|
| 12.1a | [The eye](../../campaign/act-2-homefront/level-12-storm-front/README.md#concept-art) | **pick a or b** (`tools/art/eye_l12.py`): the eye's cloud wall turning clockwise at 3°/s and its light shaft. **a** "billowing wall, cool shaft" (round billows, silver inner faces, a cool pool of light up and left); **b** "banded spiral wall, warm shaft" (spiral bands with two feeder arms, pale gold, sun glints on the swell). a is in the game | [eye-r34-a.png](../../campaign/act-2-homefront/level-12-storm-front/concept/eye-r34-a.png), [.gif](../../campaign/act-2-homefront/level-12-storm-front/concept/eye-r34-a.gif); [eye-r34-b.png](../../campaign/act-2-homefront/level-12-storm-front/concept/eye-r34-b.png), [.gif](../../campaign/act-2-homefront/level-12-storm-front/concept/eye-r34-b.gif) | either look: the game's 2 s dissolve in and out leaves a ghost of the ring over the storm at t≈149 | open |
| 12.1b | [The latch](../../enemies/air/lamprey/README.md#concept-art) | **pick a or b** (`tools/art/lamprey.py`): a Lamprey on the hull draining the shield. **a** "gulp": the body nearly still, swallowing in 0.4 s gulps, the ship's own shield ring pulsing with each gulp; **b** "siphon": the tail lashing, cyan shield motes spiralling into a teal glow at the mouth (`lamprey-drain`, no ring). a is in the game | [latch-r34-a.png](../../enemies/air/lamprey/concept/latch-r34-a.png), [.gif](../../enemies/air/lamprey/concept/latch-r34-a.gif); [latch-r34-b.png](../../enemies/air/lamprey/concept/latch-r34-b.png), [.gif](../../enemies/air/lamprey/concept/latch-r34-b.gif) | the capture found a's look "ok" at 1:1; b not seen in the game | open |
| 12.2a | [Lamprey](../../enemies/air/lamprey/README.md#concept-art) | **final art** (`tools/art/lamprey.py`, from the chosen concept): 16 headings × 4 swim frames (36 px), the latched pose at the left, right and tail spots, the stunned tumble (8 frames), the wet pop and the tatters | [lamprey-final-r34-a.png](../../enemies/air/lamprey/concept/lamprey-final-r34-a.png), [.gif](../../enemies/air/lamprey/concept/lamprey-final-r34-a.gif) | — | open |
| 12.2b | [Storm backdrop](../../campaign/act-2-homefront/level-12-storm-front/README.md#concept-art) | **final art** (`tools/art/backdrop_l12.py`, from scene-storm-r10-a on Level 11's ocean kit): the storm sea in two layers, the eye's calm chop with its seam pieces, opaque scud banks on `low-air` (≈ 12 / 22 / 35 % at the edges), two tall banks over the drifting canister, the eye's ring and shaft (look a), the breakout's light | [backdrop-final-r34-a.png](../../campaign/act-2-homefront/level-12-storm-front/concept/backdrop-final-r34-a.png), [.gif](../../campaign/act-2-homefront/level-12-storm-front/concept/backdrop-final-r34-a.gif) | big bright crests; slab-like tall banks; the GIF is 7.9 MB | open |
| 12.2c | [Level 12 props](../../campaign/act-2-homefront/level-12-storm-front/README.md#concept-art) | **final art** (`tools/art/l12_props.py`): the weather sonde (blink, collect pop), the supply canister under its drogue chute (intact, hit, spent) with its beacon glow, rain streaks, the gust arrow and streaks, lightning bolts, a lit cloud, a hidden body's glow | [l12-props-final-r34-a.png](../../campaign/act-2-homefront/level-12-storm-front/concept/l12-props-final-r34-a.png), [.gif](../../campaign/act-2-homefront/level-12-storm-front/concept/l12-props-final-r34-a.gif) | the rain stays faint at 1:1 in the game (12.7b (q)) | open |
| 12.2d | [Level 12 briefing images](../../ui/briefing/README.md#concept-art) | **final art** (`tools/art/briefing_images.py`, 672×240): Okafor's `level-12-storm-front` (the storm off the Azores winding clockwise onto the convoy lanes, the route in through the wall and the eye) and Varga's `level-12-sonde-scan` (a sonde in the rain beside a pressure plot whose curve is a regular saw) | [briefing-images-final-r34-a.png](../../ui/briefing/concept/briefing-images-final-r34-a.png) | — | open |
| 12.2e | [Lamprey intel portrait](../../ui/hangar/README.md#concept-art) | **final art** (`tools/art/intel.py`): the 30×30 portrait from the production model | [intel-final-r34-a.png](../../ui/hangar/concept/intel-final-r34-a.png) | — | open |
| 12.3a | [Level 12 in the game](../../campaign/act-2-homefront/level-12-storm-front/README.md#concept-art) | **game capture** after the fixes of 2026-10-11 (step F8; medium, the plan's fit with Rook, no `--invulnerable`): 226 / 234 kills, sondes 3 / 3, the canister broken in the t=99.5 flash, three latches (the `SHAKE OFF` · `LEFT-RIGHT` prompt on the second, shaken off), the gust arrow clear of the edge warnings, grade A, 3,024 credits; 15 frames and the whole run with sound | [level-12-capture-final-r34-a.png](../../campaign/act-2-homefront/level-12-storm-front/concept/level-12-capture-final-r34-a.png), [.mp4](../../campaign/act-2-homefront/level-12-storm-front/concept/level-12-capture-final-r34-a.mp4) | predates G8's louder shake-off (+3.5 dB); the prompt is centred in the play field (≈ 185 px from a ship at x 55); 4 of 14 thunders detectable in the mix; the plan's fit is never latched before t≈112; the video is 27.9 MB | open |
| 12.3b | [Level 12 readability](../../campaign/act-2-homefront/level-12-storm-front/README.md#concept-art) | **1:1 and 2× crops** (retaken in step G9 after the fixes): rain, gust telegraph, a Spore Bomber under a bank, the canister's glint and flash, latches, a stunned Lamprey, a sonde, the eye, the breakout light, `SONDES`, `SHIELD DRAIN`, the prompt in the play field, the debrief row | [level-12-readability-r34-a.png](../../campaign/act-2-homefront/level-12-storm-front/concept/level-12-readability-r34-a.png) | the reveal of a body under a bank was never seen (P5) | open |
| 12.3c | [Proximity Mines in play](../../campaign/act-2-homefront/level-12-storm-front/README.md#concept-art) | **supplementary clip** (step CF, 24 s with sound + sheet): the plan's L12 fit with `rear=proximity-mines:1` (t 14–30, the left flank wave) and a control run with the music and radio off (Pulse Cannon L1 + mines); the sheet plots the mix around a drop | [level-12-capture-mines-r34-a.png](../../campaign/act-2-homefront/level-12-storm-front/concept/level-12-capture-mines-r34-a.png), [.mp4](../../campaign/act-2-homefront/level-12-storm-front/concept/level-12-capture-mines-r34-a.mp4) | the **arm chirp** is in the mix (found 0.38–0.40 s after each drop at its −10 dB; a quiet tick under the guns); the **drop is not heard**: at the shots' −14 dB its click and clunk sit ≈ 10 dB under the control's mix and 15–20 dB under the real mix, masked by the front gun firing on the same tick (proposal, pending: raise the drop to the arm's level or above); the mines are small dark discs under the ship's tail, readable at 2× | open; **built since (pending review):** the drop now plays at the arming beep's −10 dB (`FlightSounds.MINE_DROPPING`), not re-captured |
| 12.4a | [Thunder](../../audio/sfx/README.md#concept-art) | **pick a or b**, three each (`tools/concept/audio/sfx_r34.py`, played in turn 0.3–1.5 s after each flash): **a** the storm bed's own thunder (FlatHill, CC0); **b** close strikes (loganzsound, CC0), a crack then the roll. a is in the game | thunder-1/2/3-r34-a and -b on the board; [Listening](#listening) | not listened to by Claude | open |
| 12.4b | [Gust](../../audio/sfx/README.md#concept-art) | **pick a or b**: **a** synthesized wind rising over the telegraph with a hollow howl; **b** a recorded gust (cobratronik, CC0) in the same envelope. Both pan from the upwind side | gust-r34-a/b | the capture heard the gust 8 of 9 times after it was raised to the warnings' level | open |
| 12.4c | [Latch](../../audio/sfx/README.md#concept-art) | **pick a or b**: **a** suck and bite (inhaling rush, the bite at 0.16 s, grinding teeth); **b** clamp on the hull (a wet splat on a metallic thud, gnawing; JustInvoke CC-BY) | lamprey-latch-r34-a/b | b's wet splat: check it is not "bubbly" | open |
| 12.4d | [Drain loop](../../audio/sfx/README.md#concept-art) | **pick a or b**, a seamless 2.4 s loop while a Lamprey drains: **a** a low electric hum throbbing with the gulps; **b** a siphoning whine with crackle (zimbot, CC-BY) | lamprey-drain-r34-a/b | — | open |
| 12.4e | [Shake-off, chime, rain bed](../../audio/sfx/README.md#concept-art) | **accept or redo**, one version each: the **shake-off** (a wet slap and a squeal whooshing away; +3.5 dB in step G8), the **sonde chime** (telemetry chirp into two bell tones), the **rain-only storm bed** (F6 = a, a 10 s loop cut from the FlatHill source) | lamprey-shake-r34-a, sonde-chime-r34-a, ambience-storm-rain-r34-a | the shake-off was marginal under the busy mix before G8 | open |
| 12.4f | [Level 12's lines voiced](../../audio/voice/README.md#decisions) | **listen**: 18 lines with the cast voices (Okafor 5, Varga 8, Rook 5), six takes pinned | [Listening](#listening) | LISTEN: Varga's "herding" (Whisper hears "hurting" in all 24 seeds), "sonde", a few takes end in a breath or murmur | open |
| 12.5 | [Level 12 texts](#level-12-texts) | **text review**: the two briefing pages (page 1 shortened, C5), Rook's teaser, Varga's four intel lines, the radio script, the HUD's words | — | the approved page 1's homing advice moved to the teaser | open |
| 12.6 | [Level 12 numbers](#level-12-numbers) | accept or change | — | 234 units, not the estimated 175–185; rear only 6 % | open |
| 12.7a | [Part F decisions](#level-12-decisions-and-build-choices) | **for the record**: F1–F9 and the defaults were the user's own (2026-10-09; F7 = b); check they were carried out | — | | open |
| 12.7b | [Level 12 build choices](#level-12-decisions-and-build-choices) | **yes or no, each**: (a)–(x) | — | (q) reads the rain's 40 % cap on what is drawn | open |

### Level 12 texts

From [Level 12's data](../../campaign/act-2-homefront/level-12-storm-front/data.yaml).

**Briefing**, two pages, one screen each:

> **Commander Okafor** (`level-12-storm-front`): "Lancer, Aegis Actual. A storm system has formed off
> the Azores, and it's moving onto the convoy lanes faster than any storm should. Vrell flyers are
> using it as cover. Clear the storm front so the next convoy can follow. Expect contacts from every
> side."
>
> **Dr. Varga** (`level-12-sonde-scan`): "The pressure readings make no sense. Three CDF weather
> sondes are still transmitting inside the front. Fly through them for me and I can tell you why."

*Approved page 1 (C5)* ended: "…so the next convoy can follow. Visibility will be poor and contacts
will come from every side. Homing weapons will help when you can't see what you're shooting."
Varga's page is unchanged.

**Hangar teaser** (Rook, not voiced): "A storm that blew up in six hours, and things in it that bite.
Homing missiles, Lancer: you won't see much in there." *Approved:* "A storm that formed in six hours.
Contacts from every side: homing weapons recommended."

**Varga's intel lines** (L2 and L3 shortened to fit the panel; the approved single line was "They'll
come from all sides, Lancer, and the cloud hides them. Homing missiles see through rain. Spread fire
for the seeds."):

| Sensor suite | Line |
|---|---|
| none | "A storm off the Azores, Lancer, and it grew in hours. Air contacts inside it." |
| L1 | "From every side. Gusts will push you sideways: watch for the wind arrows." |
| L2 | "New: an eel that drains your shield. Shake it off." |
| L3 | "Homing sees through the rain. Spread for the seeds." |

**Radio script** (the eye's lines retimed to 149.5–182.5 for the typing time; Rook's lines only while he flies):

| Trigger | Speaker | Line |
|---|---|---|
| t=2 | Okafor | "Weather service calls it the worst Atlantic storm in forty years. It formed in six hours. Stay together." |
| t=14 | Rook | "Feel that? The wind's trying to take the stick off me." |
| t=30 | Varga | "Eel-shaped contacts. They latch on and drain your shield. Shake hard to throw them off." |
| The attempt's first latch | Rook | "You've got a passenger! Shake it off!" |
| t=44 | Rook | "Seeds on the wind, left side!" |
| t=58 | Varga | "There's a CDF sonde out there. Fly through it and I can read its data." |
| t=86 | Varga | "Lightning shows what's in the clouds. Watch the flashes." |
| t=128 | Okafor | "Storm wall ahead. Instruments will be useless in there. Fly by eye." |
| t=149.5 | Rook | "Blue sky. I'd almost forgotten what it looks like." |
| t=157 | Varga | "Lancer, look at them. They're circling with the wind. They aren't caught in this storm. They're steering it." |
| t=169.5 | The Choir (distorted) | "[the Choir sings]" |
| t=176 | Okafor | "Say again, Doctor?" |
| t=182.5 | Varga | "They're herding the weather, Commander. They understand our climate better than we do." |
| t=200 | Rook | "Next time, I'm bringing an umbrella." |
| Canister broken (secret) | Varga | "A CDF supply canister. Lucky find in this soup." |
| Secondary met | Varga | "Three sondes, full data. They fed the storm, Commander. I'll have the pattern by morning." |
| Level end | Okafor | "You're through. The storm is breaking up behind you, as if someone let it go." |

**On screen**: the objective `SONDES 3` (briefing, from sensor L1); the tracker `SONDES n / 3` and the
debrief row `SONDES n / 3`; the shield row's amber `SHIELD DRAIN`; the event prompt `SHAKE OFF` ·
`LEFT-RIGHT` in the play field; the secondary "Fly through all three sondes".

### Level 12 numbers

- **Enemies** (medium **234**, easy 182, hard 287): Skitter 111, Needler 43, Whirl Seed 42, Lamprey 27,
  Wraith 5, Spore Bomber 4, Mantis 2; **70.2 a minute** (Act 2's minimum is 50); directions front 70 /
  sides 24 / rear 6 % (no rear Skitter stream is possible: only chasers stream from the rear).
- **Credits**: budget(12) **1,473**, `bounty_scale` **0.75**, typical haul **1,476** (+0.2 %), perfect
  run **2,514** (1.71 × budget); the canister 130 and the secondary 75 in Act 1 terms (pay 208 and 120).
- **Wind**: gusts from the left, 2.5 s, **40 px/s** (easy 25, hard 55), telegraph 1.0 s (easy 1.5 s);
  the storm wall's 20 px/s drift (none on easy); hard's extra gusts at t=34 and 72; none in the eye.
- **The Lamprey**: drain 8 shield a second (hard 12), the shield's regeneration paused while latched;
  hard 2 latch slots; shield cells drop ×1.5 (`drop_factor`).
- **Sondes and canister**: sondes at x 400, 70, 390 (t 60, 110, 165), 60 px/s down (≈ 9–11 s on the
  screen), collected within 36 px; the canister at (420, top) at t=99 drifting (−24, −70) px/s, ≈ 8 s,
  lit by the flashes at 99.5 and 103.5; hard 5 hits.
- **Autopilot** (seed 2185, A+ on all three): easy 2 latches, 5.3 shield drained per latch, 3 sondes;
  medium 4 / 9.5 / 3; hard 8 / 17.4, **25 armour lost** and **2 sondes** (secondary failed).
- **Sound levels**: gust and shake-off at the warnings' level (0 dB), drain loop −6 dB, chime −4 dB,
  a latch or shake-off ducks the theme 6 dB for 1 s; the theme −6 dB in the eye (F5 = a).
- **Atlas**: Level 12's own, 3 of 6 pages.

### Level 12 decisions and build choices

**Part F decisions (the user's own, 2026-10-09; row 12.7a)**: F1 = a (banks hide only `low-air`
bodies, a flash makes them see-through), F2 = a (the canister always hittable, seen in two flashes),
F3 = a (what hits a latched Lamprey goes by weapon), F4 = a (one latch per Lamprey), F5 = a (base stem
at −6 dB in the eye), F6 = a (a rain-only bed and thunder one-shots), **F7 = b** (sondes on the play
plane at 60 px/s, no magnet), F8 = a (art straight to production, a/b for the eye, the latch and four
sounds), F9 = a (a Lamprey swarm is a swarm of chasers); the storm turns **clockwise** on purpose
(user); all defaults applied (see [Level 12's Decisions](../../campaign/act-2-homefront/level-12-storm-front/README.md#decisions)).

**Build choices (row 12.7b)**, each yes or no:

- **(a)** A Lamprey touching the ship before its chase latches at once.
- **(b)** No drain during the ship's mercy time.
- **(c)** Rook touching a Lamprey is a contact (he is never latched).
- **(d)** The latch spots: left (−22, −13), right (21, −13), tail (0, −35) from the ship's centre.
- **(e)** A sonde enters and leaves 24 px outside the edge, is collected only while the ship flies, and
  is lost once wholly below the bottom edge; sondes are not allowed in a level with a boss checkpoint.
- **(f)** A placed pickup (the eye's armour patch, x 300) enters with its centre on the top edge.
- **(g)** The canister is not hit by bombs, shells or torpedoes, and is gone once off the screen.
- **(h)** **234 units** at medium (70 a minute) instead of 175–185: leaner popcorn left empty screens
  over 3 s with the plan's two Micro-missile Pods; the eye keeps four small popcorn waves.
- **(i)** `bounty_scale` 0.75 (the estimate was ≈ 0.9 at 50 a minute).
- **(j)** Rear only 6 % (the Wraiths and Lampreys); the extra popcorn from the sides.
- **(k)** Sondes at x 400 / 70 / 390; the canister's entry and drift as in the numbers.
- **(l)** The rain's density keys (none until t=10, 0.4 in sections 2–3, 1 in the wall's peak, 0 in
  the eye, 0.4 at the breakout thinning to 0).
- **(m)** The eye's radio at 149.5 / 157 / 169.5 (the Choir) / 176 / 182.5.
- **(n)** Varga's L2 and L3 intel lines shortened; the prompt's keys `LEFT-RIGHT`.
- **(o)** Lightning: the bolt at x 84 or 396 in turn, a 0.1 s sky tint, the banks tinted and seen
  through for 0.5 s; the flash reduction (0.35) dims everything but the see-through.
- **(p)** A `weather-glow` pulse (4 fps) on every `low-air` unit under the banks, the canister its own.
- **(q)** Rain: 480 streaks at density 1, longer in the wall, drawn so the brightest pixel adds 40 % of
  white (the high-air cap read on what is drawn: 0.64 of the image); still faint at 1:1.
- **(r)** The gust telegraph: the arrow at twice its size, 72 px in from the upwind edge and 60 px
  below the top, seven streaks; the upwind cue is the gust sound panned 0.3.
- **(s)** The reveal: `low-air` units and the canister drawn again over the banks at 0.6 × reveal,
  held 0.2 s, faded over 0.3 s (not seen in a capture with a unit, P5).
- **(t)** The shake-off prompt in the play field, centred (so the shake does not move it), on every
  latch of 0.3 s or more, at most 6 s each, until the attempt's first shake-off.
- **(u)** The latch, gust and shake-off at the warnings' level, the theme's 6 dB duck, the shake-off
  file +3.5 dB (G8).
- **(v)** The eye's ring and shaft fade over the backdrop's 5 s ramps (the 2 s dissolve ghost, 12.1a).
- **(w)** The plan's fit is not latched before t≈112 (the early Lampreys die first).
- **(x)** The canister breaks at once when the ship parks under it (its crate can be missed).

## Level 13 — Polar Relay (part G, files r35)

Level 13 is `design: review`; [its document](../../campaign/act-2-homefront/level-13-polar-relay/README.md),
the [Skimmer](../../enemies/naval/skimmer/README.md), [Nansen Relay and the drone](../../allies/README.md).
G1–G9 were answered **a** on the user's behalf.

### Choices — Level 13

| # | Part | What is proposed | Review files | Weak spots (Claude) | Outcome |
|---|---|---|---|---|---|
| 13.1a | [Nansen Relay](../../allies/README.md#concept-art) | **pick a or b** (`tools/art/relay.py`, production quality, 96×96): intact / damaged / critical / destroyed, the grid-online light sequence, fire, the smoke column. **a** "ice station"; **b** "grid node" (a hardened blockhouse with three phased-array faces). a is in the game | [relay-r35-a.png](../../allies/concept/relay-r35-a.png), [.gif](../../allies/concept/relay-r35-a.gif); [relay-r35-b.png](../../allies/concept/relay-r35-b.png), [.gif](../../allies/concept/relay-r35-b.gif) | a's damage smoke was unreadable on the ice until G7 made it rise and drift | open |
| 13.1b | [Whiteout](../../campaign/act-2-homefront/level-13-polar-relay/README.md#concept-art) | **pick a or b** (`tools/art/whiteout_l13.py`): the whiteout peak (176–188, easy 6 s). **a** "rolling banks"; **b** "blizzard curtain". a is in the game | [whiteout-r35-a.png](../../campaign/act-2-homefront/level-13-polar-relay/concept/whiteout-r35-a.png), [.gif](../../campaign/act-2-homefront/level-13-polar-relay/concept/whiteout-r35-a.gif); [whiteout-r35-b.png](../../campaign/act-2-homefront/level-13-polar-relay/concept/whiteout-r35-b.png), [.gif](../../campaign/act-2-homefront/level-13-polar-relay/concept/whiteout-r35-b.gif) | the banks are static images cross-faded (the roll is the game's); snow faint at medium | open |
| 13.2a | [Skimmer](../../enemies/naval/skimmer/README.md) | **final art** (`tools/art/skimmer.py`, from the chosen round-06 concept): 16 headings × 2 tail frames with the collar, the sub-pass body, a wake per heading, the flip-and-sink; with its wakes, sink and death effect in the game | [skimmer-final-r35-a.png](../../enemies/naval/skimmer/concept/skimmer-final-r35-a.png), [.gif](../../enemies/naval/skimmer/concept/skimmer-final-r35-a.gif) | the tail beats at 10 fps in the game (the art suggests 8) | open |
| 13.2b | [Arctic backdrop](../../campaign/act-2-homefront/level-13-polar-relay/README.md#concept-art) | **final art** (`tools/art/backdrop_l13.py`, from scene-arctic-r09-a; after G5b and G5c): one unique ground strip (the approach and the halt's frame), leads cut along every Skimmer lane (58 px either side), the relay's and the hut's floes, brash where the walkers cross, dark water drifting on `deep`, loose ice and the mortars' floe drifting, pale fog and the whiteout banks | [backdrop-final-r35-a.png](../../campaign/act-2-homefront/level-13-polar-relay/concept/backdrop-final-r35-a.png), [.gif](../../campaign/act-2-homefront/level-13-polar-relay/concept/backdrop-final-r35-a.gif) | **the halt now reads as the relay's floe in brash-strewn water with floes only at the side edges** (ice 20 % of the halt's frame, was 29 %); three parallel S-shaped intro leads 210 px apart; faint contour steps in the fog at 2× | open |
| 13.2c | [Drone and research hut](../../campaign/act-2-homefront/level-13-polar-relay/README.md#concept-art) | **final art**, one version each (`tools/art/l13_props.py`): the CDF supply drone (rotors, clamps, nav lights, the patch in its bay, its arc from the helipad) and the research hut (four states, the hatch beacon) | [l13-props-final-r35-a.png](../../campaign/act-2-homefront/level-13-polar-relay/concept/l13-props-final-r35-a.png), [.gif](../../campaign/act-2-homefront/level-13-polar-relay/concept/l13-props-final-r35-a.gif) | the drone also carries Level 14's patch (from the bottom edge) | open |
| 13.2d | [Level 13 briefing images](../../ui/briefing/README.md#concept-art) | **final art**: Okafor's `level-13-relay-grid` (the Arctic from above the pole, the relay and the grid it ties together, an inset of the halt) and Varga's `level-13-ice-field` (a Skimmer weaving in its lead, a Scuttler's armoured front arc, a shot at the relay with the ship in its way) | [briefing-images-final-r35-a.png](../../ui/briefing/concept/briefing-images-final-r35-a.png) | — | open |
| 13.2e | [Skimmer intel portrait](../../ui/hangar/README.md#concept-art) | **final art**: the 30×30 portrait from the production model | [intel-final-r35-a.png](../../ui/hangar/concept/intel-final-r35-a.png) | — | open |
| 13.3a | [Level 13 in the game](../../campaign/act-2-homefront/level-13-polar-relay/README.md#concept-art) | **game capture** (step G7; medium, the plan's fit with Rook, no `--invulnerable`): 193 / 208 kills, the relay at **54 %** (secondary met), secret 1 / 1, grade A, 2,776 credits; 16 frames (three from shorter runs: easy's whiteout, the damage smoke, the destroyed relay) and the whole run with sound | [level-13-capture-final-r35-a.png](../../campaign/act-2-homefront/level-13-polar-relay/concept/level-13-capture-final-r35-a.png), [.mp4](../../campaign/act-2-homefront/level-13-polar-relay/concept/level-13-capture-final-r35-a.mp4) | **predates the fixes** (G5b, G5c, G8): an intro Skimmer crossing a floe, grey fog smudges, the mortars' wreck left at the edge, the chord over the jingle; medium is tight with the plan's fit (armour 53 / 60, relay 54 %); the debrief comes ≈ 16 s after the level's end behind two radio lines | open |
| 13.3b | [Level 13 fixes](../../campaign/act-2-homefront/level-13-polar-relay/README.md#concept-art) | **supplementary clip** (step G9, 45 s with sound + sheet): the t=20 Skimmers on their leads, the mortars' wreck riding its floe (t 77), the pale fog against the old grey (t 145), the whiteout (176–186), the chord (218.05–220.65) then the jingle without overlap | [level-13-capture-fixes-r35-a.png](../../campaign/act-2-homefront/level-13-polar-relay/concept/level-13-capture-fixes-r35-a.png), [.mp4](../../campaign/act-2-homefront/level-13-polar-relay/concept/level-13-capture-fixes-r35-a.mp4) | it shows two hulls touching floe edges (t 24.5, 27.5), fixed **after** it by G5c's wider leads (recaptured in 13.3c); the wreck's remains fade in over ≈ 0.5 s | open |
| 13.3c | [Level 13 leads after G5c](../../campaign/act-2-homefront/level-13-polar-relay/README.md#concept-art) | **supplementary clip** (step CF, 22 s with sound + sheet; no firing, `--invulnerable`): the t=20 Skimmers through their whole leads (t 18–30), then the halt frame (t 40–50): the relay's floe in brash-strewn water, small floes along the side edges | [level-13-capture-leads-r35-a.png](../../campaign/act-2-homefront/level-13-polar-relay/concept/level-13-capture-leads-r35-a.png), [.mp4](../../campaign/act-2-homefront/level-13-polar-relay/concept/level-13-capture-leads-r35-a.mp4) | **every hull on open water**: a scan at 10 fps (t 18.5–30.5) finds nothing white under a hull but its wake; the nearest floe edge ≈ 15 px off (t 21.6) | open |
| 13.4a | [Relay hit](../../audio/sfx/README.md#concept-art) | **pick a or b** (`sfx_r35.py`): **a** struck plating and a spark (CC0 recordings); **b** a dull thud and the link glitching | relay-hit-r35-a/b | plays often (at −2 dB) | open |
| 13.4b | [Integrity alarm](../../audio/sfx/README.md#concept-art) | **pick a or b** (synthesized), once at 50 % and once at 25 %: **a** a hi-lo two-tone ×3; **b** three rising whoops | integrity-alarm-r35-a/b | — | open |
| 13.4c | [Grid online](../../audio/sfx/README.md#concept-art) | **pick a or b** at t=218, leading into the jingle: **a** five breakers closing, a mains hum swelling, an A-major bloom (synthesized); **b** a spin-up into a contact clunk (Glitchedtones, CC0) | grid-online-r35-a/b | the chord is as loud as the jingle (it now ends before it, C4) | open |
| 13.4d | [Drone launch, relay destroyed](../../audio/sfx/README.md#concept-art) | **accept or redo**, one version each: the **drone launch** (four rotors spinning up, climbing away; +2 dB after G7) and **relay destroyed** (a big boom, metal rumbling, a power-down) | drone-launch-r35-a, relay-destroyed-r35-a | not listened to by Claude; the capture heard all of Level 13's sounds | open |
| 13.4e | [Nansen's voice](../../audio/voice/README.md#concept-art) | **audition, pick a or b** (`tools/concept/audio/tts_r35.py`, Chatterbox, radio filter b): the CDF officer at Nansen Relay, three lines 1 s apart (t=36 arrival, the 50 % line, t=218 grid sync). **a** Patrick Eaton (LibriVox, male, 124 Hz); **b** Sharon Riskedahl (LibriVox, female, 172 Hz). His five lines play as **text** until cast | [voice-nansen-r35-a.ogg](../../audio/voice/concept/voice-nansen-r35-a.ogg), [voice-nansen-r35-b.ogg](../../audio/voice/concept/voice-nansen-r35-b.ogg); the reference clips in [Listening](#listening) | after the pick his lines are rendered and `RadioTimelineTest` rerun (his t=36 line meets Okafor's t=47.5 only if it runs over ≈ 10 s) | open |
| 13.4f | [Level 13's lines voiced](../../audio/voice/README.md#decisions) | **listen**: 15 lines (Okafor 8, Varga 4, Rook 3), three pinned; the briefing pages re-rendered after shortening | [Listening](#listening) | LISTEN: Varga's t=13 pin still has a few high syllables; "floes" sounds like "flows" | open |
| 13.5 | [Level 13 texts](#level-13-texts) | **text review**: the two briefing pages (shortened and split, C5), Rook's teaser, Varga's four intel lines, the radio script with Nansen's lines, the HUD's words | — | the shortened page 1 drops the Atlas-Seven link, "including behind you" and "if the relay falls" | open |
| 13.6 | [Level 13 numbers](#level-13-numbers) | accept or change | — | medium's relay is a knife edge (median 50 % left) | open |
| 13.7a | [Part G decisions](#level-13-decisions-and-build-choices) | **check**: G1–G9 = a and the stated defaults, taken on the user's behalf | — | G1 leaves the Stinger flying on after its dive (not a kamikaze) | open |
| 13.7b | [Level 13 build choices](#level-13-decisions-and-build-choices) | **yes or no, each**: (a)–(t) | — | (b) hard's relay is stronger than medium's | open |

### Level 13 texts

From [Level 13's data](../../campaign/act-2-homefront/level-13-polar-relay/data.yaml).

**Briefing**, two pages, one screen each (shortened and split by the main agent, C5):

> **Commander Okafor** (`level-13-relay-grid`): "Lancer, Aegis Actual. Nansen Relay ties every gun on
> the planet into one defence grid, and a Vrell landing force is crossing the pack ice to it. Hold over
> the relay for three minutes while it synchronises. They will come from every side. Aegis Actual out."
>
> **Dr. Varga** (`level-13-ice-field`): "Fast boats weave in the leads: lead your shots or spray.
> Walkers' claws are armoured in front, so hit them from the side. Shots at the relay are aimed at it:
> stand in the way. Side and rear guns will earn their keep."

*Approved (one Okafor page)*: "Lancer, Aegis Actual. Thanks to Atlas-Seven, Nansen Relay in the high
Arctic is back at full power. It ties every gun, radar and satellite on the planet into one defence
grid, and the Vrell have worked that out. A landing force is moving on it across the pack ice: fast
boats in the leads between the floes, and walkers on the floes themselves. Hold position over the
relay for three minutes while it synchronises the grid. They will come from every side, including
behind you. If the relay falls, we lose the grid. Aegis Actual out."

**Hangar teaser** (Rook, not voiced; unchanged): "Defend the Arctic relay for three minutes. Attacks
from every edge: rear and side guns pay off."

**Varga's intel lines** (the approved single line: "They'll come from all four edges, Lancer. Side and
rear guns will earn their keep, and spread fire for the boats."):

| Sensor suite | Line |
|---|---|
| none | "Pack ice, Lancer. Boats in the leads, walkers on the floes, and no front line out there." |
| L1 | "They'll come from all four edges. Side and rear guns will earn their keep." |
| L2 | "New: the Skimmer, a fast boat that weaves. The walkers are back, and the divers." |
| L3 | "Spread fire for the boats, side shots for the walkers. Hold the relay for three minutes." |

**Radio script** (Nansen's lines as text with the generic CDF portrait until he is cast; Rook's only while he flies):

| Trigger | Speaker | Line |
|---|---|---|
| t=1 | Okafor | "Nansen Relay coordinates every gun on the planet. If it goes dark, so does the grid." |
| t=13 | Varga | "Fast boats in the leads. They weave. Lead your shots, or spray." |
| t=24 | Rook | "Cold enough for you, Lancer? My canopy's frosting on the inside." |
| t=36 | Nansen | "Aegis, Nansen. Very glad to see you. They come at us from every direction out here." |
| t=47.5 | Okafor | "Lancer, you're holding station over the relay. Nothing touches it." |
| t=58.5 | Rook | "Contacts, all sides! Pick a direction, any direction." |
| t=102 | Varga | "Walkers on the floes. Their claws are armoured from the front. Hit them from the side." |
| t=154 | Okafor | "Divers inbound. They're going for the relay, not you." |
| t=162 (the first drone's release) | Nansen | "Supply drone away. Catch it, Aegis." |
| t=172 | Okafor | "Weather's turning. Whiteout coming in from the edges." |
| t=179 | The Choir (distorted) | "[the Choir sings]" |
| Relay below 50 % | Nansen | "Integrity at half! We can't take much more!" |
| Relay below 25 % | Okafor | "Lancer, the relay is failing. Everything else can wait." |
| t=218 | Nansen | "Grid sync complete. Every battery on Earth just came back online." |
| Secondary met | Nansen | "Integrity holding above half. You kept the lights on, Aegis." |
| t=233.5 | Okafor | "Well held, Aegis. Now we can see the whole board." |
| Hut's cache (secret) | Varga (new) | "A CDF research cache. Somebody left that hut in a hurry." |
| Level end | Rook | "Next time they need a relay defended, I'm asking for one with palm trees." |
| Relay destroyed (mission failed) | Okafor | "Nansen is gone. Pull back, Lancer." |

**On screen**: the objective `DEFEND NANSEN RELAY`; tracker line 1 `RELAY` with the integrity bar (a
tick at 50 %, amber below it) and the countdown from `2:58`; line 2 `INTEGRITY > 50 %` (the font has no
`≥`: a `>` with a one-pixel bar under it), then `DONE` or `FAILED`; the debrief row `RELAY n %` (rounded
down); the secondary "Keep the relay at 50 % or more".

### Level 13 numbers

- **The halt**: an approach of ≈ 5,460 px, the halt eased in over 2 s at t≈38–40, the synchronisation
  t=40–218 (178 s), an exit of ≈ 1,960 px; the relay at x 240, 290 px up, an 80×80 hit box.
- **Enemies**: easy 160, medium **208**, hard 260 (the plan's 196; popcorn as streams of 4 for the 3 s
  pacing rule); **54.3 a minute**.
- **The relay** (guarding autopilot, 8 seeds per difficulty):

  | Difficulty | Damage taken | Integrity | Left at t=218 | Secondary met |
  |---|---|---|---|---|
  | Easy | 48–56 | 340 (approved 900) | 84–86 % | 8 of 8 |
  | Medium | 112–124 | 240 (approved 600) | 48–53 % (median 50 %) | 7 of 8 |
  | Hard | 386–410 | 520 (approved 450) | 21–26 % | 0 of 8 |

  Nearly all of it is the Scuttlers' fans in arc. A human letting the walkers fire loses medium's secondary.
- **Credits**: budget(13) **1,577**, `bounty_scale` **0.67**, typical haul **1,606** (+1.8 %), perfect
  run **2,466** (1.56 ×); the hut's cache 110 and the secondary 95 in Act 1 terms (pay 176 and 152).
- **Whiteout**: 176–188 at medium (4 s ramps), easy 6 s (179–185), the wind +3 dB at the peak; snow on
  high-air 0 / 30 % / full by the atmosphere.
- **Sounds**: relay hit and drone launch −2 dB (the drone +2 dB after G7), alarm, grid online and relay
  destroyed at the warnings' level.
- **`Level13Test`**: A+ / A+ / A (easy / medium / hard).

### Level 13 decisions and build choices

**Part G decisions on the user's behalf (row 13.7a; [Level 13's Decisions](../../campaign/act-2-homefront/level-13-polar-relay/README.md#decisions))**:
**G1 = a** the hook modes exact on the relay-aimed rule (`alternate`, `in-arc`, `always`; the Stinger
dives, hits once and flies on); **G2 = a** the waves kept, the integrity tuned per difficulty; **G3 = a**
halted time counts as scroll time (≈ 190 units from all edges); **G4 = a** Skimmers walk ground lanes in
fixed leads, loose ice drifts; **G5 = a** `water: true` for the whole field, the water splash by unit;
**G6 = a** the supply drone is presentation, releasing an ordinary patch; **G7 = a** the whiteout hides
ground units at the edges, air stays visible; **G8 = a** one jingle at the sync's end, the debrief
without; **G9 = a** a/b for the relay, the whiteout, three sounds and Nansen, the rest straight to
production. **The stated defaults**: the halt as a speed-0 section with 2 s eases; the defend primary on
the escort's machinery; the hook configuration per kind (easy Stingers unhooked); the secondary
`integrity: 0.5`; data in Act 1 terms with a `bounty_scale`; ≥ 50 a minute, 3 s edge warnings; the
Scuttlers' lanes ending short of the relay; the mortars' floe from the left at t=66; the Needler circle
over the relay; the radio retimed (Rook's t=82 and t=122 dropped); the HUD's `RELAY` tracker; the
atmosphere split at t=140; "Homefront" from section 2; the threat profile from the data (a `defend`
level is excepted from the 60 %-from-the-top rule); the plan's L13 visit as written; Rook does not guard
the relay; L13's own atlas.

**Build choices (row 13.7b)**, each yes or no:

- **(a)** The Stinger's fan under `always` keeps its 0.6 s timing (it fires from below the relay); the
  relay's lob also hurts the ship inside its circle.
- **(b)** The integrity **240 / 340 / 520** (hard above medium: hard's load is 3.4 ×).
- **(c)** 208 units; the rear popcorn as Skitter line abreasts, the t=140 rear stream as a snake on a
  path, the t=200 rear Needlers a line abreast.
- **(d)** `bounty_scale` 0.67.
- **(e)** Traits `spread` and `rear` (`side` only in the notes: three do not fit the panel).
- **(f)** The secret's line is Varga's (new); Nansen uses the generic CDF portrait.
- **(g)** Grid online at t=218 (the README's t=220 read as the sync's completion) and the jingle at
  220.6 after the chord (C4).
- **(h)** The countdown from 2:58, rounding up; `>` with a bar for `≥`; `RELAY n %` rounded down; the
  relay's HUD pip unused.
- **(i)** The drone swings 110 px out (the first right, the second left), 4 s each way, only while the
  relay stands.
- **(j)** The relay's damage smoke rising and drifting below 50 %, fire below 25 %, a smoke column when
  destroyed.
- **(k)** The Skimmer's tail at 10 fps; a burst within 8 px of a Skimmer is the water splash, every other
  burst on the ice the ground's.
- **(l)** Snow on high-air by the atmosphere (0 / 30 % / full), at most 40 % a flake.
- **(m)** The ground as one unique strip (a halt shows section 1's ground); the leads ring the relay's
  floe; the Scuttlers wade across the leads (brash rubble chokes there).
- **(n)** Loose ice drifting 6 px/s in the leads' outer margins; the mortars' floe at 10 px/s overlapping
  the relay floe's edge; static fog and whiteout banks cross-faded.
- **(o)** Three parallel S-shaped intro leads 210 px apart (G5b; a data change would merge them).
- **(p)** The leads 58 px either side (G5c): the halt is mostly water, floes only at the side edges.
- **(q)** The fog lighter than the round-35 render (pale, soft-edged).
- **(r)** The mortars' wreck drifts on with its floe (`WRECK_DRIFT`, presentation only); its remains fade
  in over ≈ 0.5 s.
- **(s)** The level ends ≈ 16 s before the debrief (t 216–251 without enemies, two radio lines).
- **(t)** Medium stays tight with the plan's fit (the capture: armour 53 / 60, relay 54 %).

## Level 14 — Siege Spire and the Act 2 end (part H, files r36)

Level 14 is `design: review`; [its document](../../campaign/act-2-homefront/level-14-siege-spire/README.md),
the [Siege Spire](../../enemies/bosses/siege-spire/README.md), the [Act 2 end](../../campaign/act-2-homefront/README.md).
H1–H10 were answered **a** on the user's behalf; **H1 changes an approved spec** (C1).

### Choices — Level 14

| # | Part | What is proposed | Review files | Weak spots (Claude) | Outcome |
|---|---|---|---|---|---|
| 14.1a | [Root burst](../../campaign/act-2-homefront/level-14-siege-spire/README.md#concept-art) | **pick a or b** (`tools/art/root_burst.py`): the 1 s telegraph and the eruption that grows a Spine Turret and a Polyp Mortar. **a** "breaking paving" (cracks with violet glow, dust, a slate root arching out); **b** "creep surge" (creep tendrils, spore mist, a sac mound splitting into buds). a is in the game | [root-burst-r36-a.png](../../campaign/act-2-homefront/level-14-siege-spire/concept/root-burst-r36-a.png), [.gif](../../campaign/act-2-homefront/level-14-siege-spire/concept/root-burst-r36-a.gif); [root-burst-r36-b.png](../../campaign/act-2-homefront/level-14-siege-spire/concept/root-burst-r36-b.png), [.gif](../../campaign/act-2-homefront/level-14-siege-spire/concept/root-burst-r36-b.gif) | — | open |
| 14.1b | [Daylight](../../campaign/act-2-homefront/level-14-siege-spire/README.md#concept-art) | **pick a or b** (`tools/art/daylight_l14.py`, stills): the 12 s after the kill. **a** "golden hour" (warm grade, low-sun beams through the tearing canopy); **b** "pale dawn" (cool grade, haze burning off the lake, no beams). a is in the game | [daylight-r36-a.png](../../campaign/act-2-homefront/level-14-siege-spire/concept/daylight-r36-a.png); [daylight-r36-b.png](../../campaign/act-2-homefront/level-14-siege-spire/concept/daylight-r36-b.png) | stills only; the game's daylight at 25 px/s is in 14.3d | open |
| 14.2a | [Siege Spire](../../enemies/bosses/siege-spire/README.md#concept-art) | **final art** (`tools/art/siege_spire.py`, from the chosen concept): the trunk (3 maw steps), six roots alive and dead, pods (32 headings), mortars, husks, the stump, the lift, the free spire (16 headings over 60°), the emitter and maw glows, debris; ≈ 41 % of an atlas page | [siege-spire-final-r36-a.png](../../enemies/bosses/siege-spire/concept/siege-spire-final-r36-a.png), [.gif](../../enemies/bosses/siege-spire/concept/siege-spire-final-r36-a.gif) | the free spire is big and sat on the stump (the stump is now drawn as ash, H7b); the canopy's tear is subtle | open |
| 14.2b | [The Spire's death](../../enemies/bosses/siege-spire/README.md#concept-art) | **final art** (`tools/art/siege_spire_death.py`, 8 frames over the 3 s chain): the core's light cracking out along the six petals, then the chunks flying apart and burning down; the game's bursts over it | [siege-spire-death-final-r36-a.png](../../enemies/bosses/siege-spire/concept/siege-spire-death-final-r36-a.png), [.gif](../../enemies/bosses/siege-spire/concept/siege-spire-death-final-r36-a.gif) | a 2D break-up; the act bosses' full-screen white flash at the end is kept (as Level 07's) | open |
| 14.2c | [Geneva backdrop](../../campaign/act-2-homefront/level-14-siege-spire/README.md#concept-art) | **final art** (`tools/art/backdrop_l14.py`, from scene-geneva-r10-a; after H9's slower daylight): the lakefront, the old town, the root field, the Concord plaza under the anchor, the daylight tiles; the Rhône with the Pont du Mont-Blanc, Saint-Pierre, the Molard, the Jet d'Eau, the Ravagers' crater, Hammer flight; the canopy's veins and tear, the beams | [backdrop-final-r36-a.png](../../campaign/act-2-homefront/level-14-siege-spire/concept/backdrop-final-r36-a.png), [.gif](../../campaign/act-2-homefront/level-14-siege-spire/concept/backdrop-final-r36-a.gif) | no `deep` layer, the lake painted on the ground with the shadow baked; the daylight graded by position along the strip; static lamps and Jet d'Eau; one wide bridge; 4 tear and 2 beam frames | open |
| 14.2d | [Armoured car](../../campaign/act-2-homefront/level-14-siege-spire/README.md#concept-art) | **final art** (`tools/art/l14_props.py`): the CDF car under creep, cleared hit by hit, the hatch open with the crate | [l14-props-final-r36-a.png](../../campaign/act-2-homefront/level-14-siege-spire/concept/l14-props-final-r36-a.png), [.gif](../../campaign/act-2-homefront/level-14-siege-spire/concept/l14-props-final-r36-a.gif) | its hit flash is faint in the game | open |
| 14.2e | [Act 2 outro images](../../campaign/act-2-homefront/README.md) | **final art** (`tools/art/outro_l14_images.py`): the four outro pages: the stump in Geneva at sunrise (the production stump at 0.40 scale), the wall of names at dawn, Ares Landing's signal from Mars, Aegis Wing and the Mars transport at dusk | [act-2-outro-final-r36-a.png](../../campaign/act-2-homefront/level-14-siege-spire/concept/act-2-outro-final-r36-a.png) | — | open |
| 14.2f | [Level 14 briefing images](../../ui/briefing/README.md#concept-art) | **final art**: Okafor's `level-14-spire-over-geneva` (the Spire rising out of the Concord rotunda into the canopy, the route) and Varga's `level-14-root-scan` (the root grid, a burst in detail, a Wraith from behind) | [briefing-images-final-r36-a.png](../../ui/briefing/concept/briefing-images-final-r36-a.png) | — | open |
| 14.2g | [Spire intel silhouette](../../ui/hangar/README.md#concept-art) | **final art**: the 40×40 boss silhouette, the rooted body as the arena first shows it | [intel-final-r36-a.png](../../ui/hangar/concept/intel-final-r36-a.png) | rooted, not the free spire | open |
| 14.3a | [Level 14 in the game](../../campaign/act-2-homefront/level-14-siege-spire/README.md#concept-art) | **game capture** (step H7; medium, a strong fit with Rook, **`--invulnerable`**): 115 / 117 kills, ROOTS 8 / 8, secret 1 / 1, the Spire in 1:26 (par 2:10), A+; 19 frames and the whole run with sound through the act end | [level-14-capture-final-r36-a.png](../../campaign/act-2-homefront/level-14-siege-spire/concept/level-14-capture-final-r36-a.png), [.mp4](../../campaign/act-2-homefront/level-14-siege-spire/concept/level-14-capture-final-r36-a.mp4) | **predates H7b and H9** (stale phase lines, the roots line over the arrival, the stump scrolling off, Hammer over the car); invulnerable (P2) | open |
| 14.3b | [Level 14 fixes](../../campaign/act-2-homefront/level-14-siege-spire/README.md#concept-art) | **supplementary capture** (step H8): Hammer at x 130, the growths firing ≈ 290 px above the bottom, Rook's warning bark and phase-3 line, the ash stump under the free spire, the withering 3 s after the kill, the daylight at 40 px/s; the bottom row a run **without** `--invulnerable` on the plan's fit, lost five times | [level-14-capture-fixes-r36-a.png](../../campaign/act-2-homefront/level-14-siege-spire/concept/level-14-capture-fixes-r36-a.png), [.mp4](../../campaign/act-2-homefront/level-14-siege-spire/concept/level-14-capture-fixes-r36-a.mp4) | predates H9 (the roots line, 25 px/s, the drone: see 14.3d); the growths still crowd a ship standing under them | open |
| 14.3c | [The act end](../../campaign/act-2-homefront/README.md) | **capture** from the same run (a real kill): the daylight and the closing line, the debrief, the act summary, the four voiced outro pages, the hangar before L15 | [act-2-outro-capture-r36-a.png](../../campaign/act-2-homefront/level-14-siege-spire/concept/act-2-outro-capture-r36-a.png) | the act summary page is silent (as Act 1's) | open |
| 14.3d | [Level 14 after H9](../../campaign/act-2-homefront/level-14-siege-spire/README.md#concept-art) | **supplementary clip** (step CF, 47 s with sound + sheet; the H7 recipe, `--invulnerable`): ROOTS 8 / 8 at ≈ 94.5 and Okafor's grid line, the CDF supply drone carrying the armour patch in from the bottom edge (t 96.5–103), the Choir and Varga after the line; the kill at ≈ 194.5 through the daylight at 25 px/s to the debrief | [level-14-capture-daylight-r36-a.png](../../campaign/act-2-homefront/level-14-siege-spire/concept/level-14-capture-daylight-r36-a.png), [.mp4](../../campaign/act-2-homefront/level-14-siege-spire/concept/level-14-capture-daylight-r36-a.mp4) | the grid line plays at ≈ 95–104, the Choir ≈ 105, Varga ≈ 111 over the warning (as H9 planned); the drone climbs to its stop, releases the patch at ≈ 100 and leaves downwards past the ship by ≈ 104; **the stump stays on the screen through the beams** (≈ 200–211): its body leaves the bottom edge at ≈ 212, the last root tips at ≈ 215.3 (≈ 21 s after the kill, the estimate was 15 s), the debrief at ≈ 217.5 | open |
| 14.4a | [Root burst sound](../../audio/sfx/README.md#concept-art) | **pick a or b** (`sfx_r36.py`): **a** paving cracking under a rumble, a masonry crunch at 1.0 s; **b** a wet creep surge and a creaking root, a wet tear at 1.0 s | root-burst-r36-a/b | **b has a "Squelch" layer: bubbly risk** | open |
| 14.4b | [Tear-free roar](../../audio/sfx/README.md#concept-art) | **pick a or b**: **a** a tectonic rip, root snaps, a shockwave boom, a deep roar under the lift; **b** a creature bellow rising with the lift, wet tearing, an air shockwave | tear-free-roar-r36-a/b | b shares its dragon roar with the Ravager's pounce b and its sea-creature roar with the Kraken's death | open |
| 14.4c | [Canopy withering](../../audio/sfx/README.md#concept-art) | **pick a or b**, 3 s after the kill: **a** a dry crackling shrivel and a far tear into wind; **b** a dying exhale, a groan and sigh into wind | canopy-withering-r36-a/b | its first second is masked by the death chain | open |
| 14.4d | [Root growth, canopy hum](../../audio/sfx/README.md#concept-art) | **accept or redo**, one version each: the **root growth** (a stretching creak and a knock) and the **canopy hum** (an 8 s loop on 46 Hz, the level's ambience, cut at the kill as the city's rises). The maw reuses the Brood Carrier's sac (0.85 pitch), the sweep the Mantis's | root-growth-r36-a, canopy-hum-r36-a | the hum is mostly under 200 Hz (small speakers lose it) | open |
| 14.4e | [Level 14's and the outro's lines voiced](../../audio/voice/README.md#decisions) | **listen**: 20 lines (Okafor 11 with the four outro pages, Varga 6, Rook 2, Hammer Lead 1), three pinned | [Listening](#listening) | LISTEN: Varga's t=102 sits high (259 Hz); Hammer Lead's "flight"; "Ares", "Root grid" | open |
| 14.5 | [Level 14 texts](#level-14-texts) | **text review**: the two briefing pages (rewritten and split, C5), Rook's teaser, Varga's four intel lines, the radio script, the four outro pages (approved text, unchanged), the screens' words | — | the approved briefing's "Tonight the Coalition puts everything it has left into the air" moved to the t=1 line | open |
| 14.6 | [Level 14 numbers](#level-14-numbers) | accept or change | — | hard's fight over the window (C6); the bounty share (C8) | open |
| 14.7a | [Part H decisions](#level-14-decisions-and-build-choices) | **check**: H1–H10 = a and the stated defaults, taken on the user's behalf | — | H1 (C1) | open |
| 14.7b | [Level 14 build choices](#level-14-decisions-and-build-choices) | **yes or no, each**: (a)–(w) | — | (c) the growths' first shot | open |

### Level 14 texts

From [Level 14's data](../../campaign/act-2-homefront/level-14-siege-spire/data.yaml) and the
[Act 2 data](../../campaign/act-2-homefront/data.yaml).

**Briefing**, two pages, one screen each (shortened by the main agent from step H5's versions, C5):

> **Commander Okafor** (`level-14-spire-over-geneva`): "Lancer, Aegis Actual. Something grew out of
> Geneva Concord two nights ago: the Siege Spire, three kilometres tall and still growing. Your flight
> goes in first, along the lakefront and through the old town to the rotunda. Destroy it. Aegis
> Actual out."
>
> **Dr. Varga** (`level-14-root-scan`): "The Vrell fight as one, and I believe the Spire binds them.
> Its roots run under the old town and grow guns where they break the surface. Kill the roots and the
> trunk starves. Bring anti-ground and something that pierces. When the crown opens, look behind you."

*Approved (one Okafor page)*: "Lancer, Aegis Actual. Two nights ago something grew out of the heart of
Geneva Concord. It is three kilometres tall and it is still growing. We call it the Siege Spire. The
Vrell across Earth fight as one, and Dr. Varga believes the Spire is what binds them. Its roots run
under the whole old town, and they grow guns where they break the surface. Tonight the Coalition puts
everything it has left into the air. Your flight goes in first: along the lakefront, through the old
town, to the Concord rotunda where the trunk stands. Destroy the Spire. Aegis Actual out."

**Hangar teaser** (Rook, not voiced): "Roots first, flyers from behind, then the Spire. I'll take the
six o'clock." *Approved:* "The Siege Spire. Roots on the ground, flyers from behind, then a fight in the
open. Anti-ground and piercing recommended."

**Varga's intel lines** (L2 and L3 shortened; the approved single line: "Bring anti-ground for the roots
and something that pierces. When the crown opens, look behind you."):

| Sensor suite | Line |
|---|---|
| none | "Geneva Concord, Lancer. Something has grown here for two nights, on the ground and in the air." |
| L1 | "Mostly from ahead. Roots break the streets: watch for the dust." |
| L2 | "Root guns, hive nodes, cloaked Wraiths. And the Spire." |
| L3 | "Bring anti-ground. When the crown opens, look behind you." |

**Radio script** (retimed in step H6; with H9's rule the root-grid line goes ahead, C10):

| Trigger | Speaker | Line |
|---|---|---|
| t=1 | Okafor | "All units, Aegis Actual. Everything the Coalition has left is in the air over Geneva tonight. Make it count." |
| t=13 | Hammer Lead | "Hammer flight on station. Call your targets, Aegis." |
| t=21 | Rook | "Lancer, whatever happens in there, I'm on your wing. As usual." |
| t=31.5 | Varga | "The canopy is one organism. The roots feed the trunk. Kill the roots and the trunk starves." |
| t=54 | Varga | "Hive nodes in the Molard. Hardened, like Lagos." |
| t=62 | Okafor | "Roots surfacing in the streets! They're growing guns." |
| Armoured car's cache (secret) | Okafor | "That car's crew left their supplies behind. They'd want you to have them, Lancer." |
| Secondary met (≈ 93.5) | Okafor | "Root grid in the old town destroyed. The engineers will thank you." |
| t=96 (≈ 104 after the root-grid line) | The Choir (distorted) | "[the Choir sings]" |
| t=102 (≈ 110) | Varga | "It's singing to the whole city. Lancer, that's the Spire." |
| t=108 (the boss warning) | Rook | his boss-warning bark (not a scripted line) |
| Phase 2 (Maw) | Varga | "The crown is opening. It's launching flyers, and they'll loop behind you." |
| Phase 3 (Free spire) | Okafor (fierce) | "It's tearing loose! Stay on it, Lancer!" |
| Phase 3, after Okafor's | Rook | "Roots and all. Somebody call a gardener." |
| Spire destroyed | Varga | "The canopy's dying. All of it, all at once." |
| Level end (the closing line) | Okafor | "Every Vrell on Earth just lost its voice. They're scattering. We take the fight to them now." |

**The Act 2 outro** (four voiced pages, Okafor; H7 = a: the approved act debrief, one paragraph a page, unchanged):

> 1 (grim, `act-2-outro-stump`): "The Siege Spire is dead. Within the hour every Vrell force on Earth
> lost its coordination. Some are digging in, most are scattering, and the army is clearing them city
> by city."
>
> 2 (grim, `act-2-outro-names`): "Nova Lagos is counting its losses. So is Geneva. So are we. Remember
> the names, then get some sleep."
>
> 3 (grim, `act-2-outro-mars`): "An hour ago Ares Landing sent a priority signal. Mars is under siege,
> and the colonies have been holding alone for three weeks."
>
> 4 (fierce, `act-2-outro-aegis`): "We take the fight to them now. Aegis Wing ships out for Mars in
> seventy-two hours. Aegis Actual out."

**On screen**: the objective `DESTROY THE SIEGE SPIRE`; the boss bar `SIEGE SPIRE`; the tracker
`ROOTS n / 8` (easy `n / 4`), then `DONE` or `FAILED`, and the debrief row `ROOTS n / 8`; the secondary
"Destroy every root-burst growth"; the act summary `ACT II COMPLETE · HOMEFRONT` (Levels 08–14, DATA
CORES `NONE FOUND`); the hangar after the outro `BEFORE L15 - NOT BUILT YET` (H9 = a).

### Level 14 numbers

- **Scroll**: 140 px/s; the root field (section 3) **80 px/s** (C2); the arena a halt at t=108–≈ 250;
  the daylight 12 s at **25 px/s** (C3); 13,020 px of ground to the arena, 13,320 in all.
- **Enemies to the arena**: easy 81, medium **112** (95 wave units and the ground units), hard 142;
  **67.6 a minute**; only the 12 s daylight is quiet.
- **Root bursts** at t=63 / 74 / 84 / 92, x 150 / 330 / 170 / 320, 100–140 px below the top edge; the
  growths 52 px apart; the first shot **0.75 s** after growing (hard 0.5 s), ≈ 290 px above the bottom
  edge; `ROOTS` 8 (easy 4), the secondary 63 in Act 1 terms (pays 101).
- **The Spire**: HP **12,040** (pods 665, mortar roots 775, maw 2,880, core 4,950; easy 9,030, hard
  15,652); the fight (autopilot, five seeds) medium **129.5 s** (126.1–131.7), **par 130 s**, easy
  95.8 s, hard **195 s** (4 of 5 won); full level runs 97 / 136 / 204 s (C6).
- **Its attacks**: pods' aimed orbs (150 px/s, 6) every 1.6 s staggered; the mortar roots' lob every 4 s
  (a 1 s marker, a 40 px impact for 10, a ring of 10, hard 12); the free spire's lob and sweep (120°,
  hard 180°, a 0.6 s telegraph, a 300 × 8 px beam for 1.2 s) every 2.5 s in turn; the maw open all
  phase 2, launches of 2 Wraiths every 8 s (hard 3), at most 4 alive; the lift 3 s to (240, 120).
- **Credits**: budget(14) **1,687**, `bounty_scale` **0.76**, typical haul **1,703** (+1.0 %), perfect
  run **2,397** (1.42 ×); the Spire 372 in Act 1 terms, **452 paid = 26.8 %** (C8); its Wraiths budgeted
  at 6 (three launches of 2); the car's crate 88 (pays 141).
- **Atlas**: Level 14's own, 5 of 6 pages.

### Level 14 decisions and build choices

**Part H decisions on the user's behalf (row 14.7a; [Level 14's Decisions](../../campaign/act-2-homefront/level-14-siege-spire/README.md#decisions))**:
**H1 = a** no creep, the arena a halt (C1); **H2 = a** data-driven root bursts (silent invulnerable
growths, 100–400 px below the top, not within 60 px of the ship's x); **H3 = a** the maw's Wraiths as
ambush launches with a 3 s bottom-edge warning, at most 4 alive; **H4 = a** one boss and one bar, a
phase `move` lifting it in ≈ 3 s, the stump from the boss's state; **H5 = a** the boss attacks `lob` and
`sweep` on the enemy code; **H6 = a** Act 1's act-end order (C13); **H7 = a** four outro pages with
images and voices; **H8 = a** a/b for the root burst, the daylight, the roar and the withering, the rest
straight to production; **H9 = a** after the outro the hangar reads `BEFORE L15 - NOT BUILT YET` (M6
owns the release's end); **H10 = a** the Spire built with the spec's HP, measured and tuned (C6, C8, C9).
**The stated defaults**: arrival, warning and checkpoint at t=108 (C12); the creep-free sections;
primary `reach-end` with the arena as its condition; the secondary `groups` of the 8 growths; popcorn
from t=8 at ≥ 50 a minute; the t=82 and t=104 rear Wraiths kept; the Ravagers' crater a burst without
growths; data in Act 1 terms with a `bounty_scale`; the act HP factor for the returning medium units
(Creeper 40, Hive Node 81, Ravager 20, Wraith 19); the four intel lines and two briefing pages; Rook's
t=80 line dropped; the drone's patch a placed pickup; edge warnings 3 s; the daylight without enemies
or music, the closing line queued; "Firestorm" from section 2; the plan's L14 visit unchanged; the
threat profile from the data; L14's own atlas.

**Build choices (row 14.7b)**, each yes or no:

- **(a)** A burst's `t` is its telegraph's start; the burst scrolls; the growths appear ≈ 140 px lower
  1 s later; a burst within 60 px of the ship's x shifts away.
- **(b)** Growing growths are invulnerable (shots glance) and silent until their first shot.
- **(c)** The growths' first shot after 0.75 s (hard 0.5 s): ≈ 290 px above the bottom edge, still
  crowding a ship standing under them.
- **(d)** The boss attacks reuse the enemy patterns' names (`mortar`, `laser-sweep`); the lob's impact
  59 steps after its marker; the sweep centred on the ship's bearing, alternating sides, hurting once per
  sweep.
- **(e)** The maw's launch lanes evenly spread (C7); four planned launch sets cycled; the warning at
  least 3 s; at most 4 alive (excess dropped); the maw window always open.
- **(f)** The lift eased (smoothstep), the layer flipping to `air` at its end; the stump is the
  destroyed and armoured parts; the armour patch drops at phase 3.
- **(g)** The anchor 196 px below the top edge, the free station (240, 120); the trunk armoured (the
  stump), the maw before the core in the hit order.
- **(h)** Easy's t=82 Wraiths replaced by a Needler V-wing from the front; the hard Molard nest of three
  Spine Turrets.
- **(i)** Rook's phase-3 line queued after Okafor's (a `PHASE` line, stale after 20 s); the car's secret
  line given to Okafor.
- **(j)** The burst positions chosen for the backdrop; the car at t=14.5, x 330; the drone's patch at
  t=100, x 240, carried in by Level 13's drone from below the bottom edge (no launch sound).
- **(k)** The radio retimed (C12) and the root-grid line ahead of the Choir (C10): Varga's "that's the
  Spire" over the Spire's arrival.
- **(l)** The pods snap to the ship's bearing (no turn rate); the crown opens over 0.5 s; the maw
  flares at each launch; the free spire turns at 2 frames a second.
- **(m)** The tear-free shockwave: ten dust puffs out to 170 px and sixteen slate shards over 1.5 s.
- **(n)** The sweep's telegraph a pulsing crimson line, its beam a crimson band with a hot core.
- **(o)** The death: eight frames over the 3 s chain, fourteen bursts, three blasts, the act bosses'
  white flash (kept, as Level 07's), the credit shower.
- **(p)** The stump drawn as ash (60 % toward grey) under the free spire.
- **(q)** The daylight on real seconds: the canopy's veins fading 2–7 s, the tear 0.5–8.5 s, the beams
  2.5–5 s; the stump ≈ 15 s on the screen at 25 px/s (estimated).
- **(r)** The canopy hum fading over 2 s at the kill, the city's ambience rising over 7 s; the withering
  3 s after the kill.
- **(s)** Hammer flight at x 130 over the lake (clear of the car), t≈12–20.
- **(t)** The act summary page silent (as Act 1's).
- **(u)** The backdrop's choices (14.2c weak spots) and the intel silhouette rooted.
- **(v)** `--loadout` takes `generator=`, `shield=`, `armor=` (the debug start can fit the plan's cores).
- **(w)** The main capture flown `--invulnerable` (P2).

## Part I — the close-out

The [close-out](../../tech/roadmap/README.md#m5-parts) ticked the Act 2 checklists, retagged what
belongs to later acts and ran Acts 1–2 as one campaign. Its gap list is `part-i-gaps.md` (repo root).

| # | Part | What is proposed | Review files | Weak spots (Claude) | Outcome |
|---|---|---|---|---|---|
| I.1 | [Campaign playthrough](../../player/wingmen/README.md) | **accept or change**: `ActPlaythroughTest` flies Levels 01–14 as one campaign on every difficulty, Rook from L08 with repairs; every level won at the first attempt; `BalanceTest` and `balance.py` print Rook's DPS (12 / 15) | — | P1, P3, P6 | open |
| I.2 | [Retagged items](../../campaign/README.md) | **yes or no**: Rook's guns and barks tables rendered by `sync_tables.py` → later: M6 (the hand-written tables match the data today); the campaign sequence 15→50, threat profiles 15–50, the `beam` and `shield-breaker` rows, unused movement and attack patterns (`submerged ambush`: Act 4) and later variety checks → Acts 3–7 | — | | open |
| I.3 | [Act 2 variety](../../enemies/README.md) | **accept**: Act 2 passes the variety checklist as built (the table corrected: no `large` unit in Act 2) | — | | open |
| I.4 | [Voice listen-through](#listening) | **human item (P8)**: the user listens to Act 2's voiced lines | [Listening](#listening) | none listened to by Claude | open |
| I.5 | Playthrough | **human item (P9)**: the user plays Act 2 | — | | open |
| I.6 | [M5 itself](../../tech/roadmap/README.md#m5-parts) | after this round: Levels 12–14 `approved`, the art `final`, M5 `done`; the `sub` layer's three-OS check after the push (P7) | — | | open |

## Listening

The voiced lines (rows 12.4f, 13.4f, 14.4e) as the game plays them from `assets/voice/`, matched by
the voice key in `pipeline/build/voice/lines.json`; LISTEN marks the lines worth a listen (Whisper and
pitch measurements only). Then Nansen's reference clips (row 13.4e) and the sounds with their listening
points (the a/b and one-version files are also on the board's cards).

| Level | Trigger | Speaker | Text | Take |
|---|---|---|---|---|
| L12 | briefing page 1 | Okafor | "Lancer, Aegis Actual. A storm system has formed off the Azores, and it's moving onto the convoy lanes…" — LISTEN: "Azores", "Vrell" | [play](../../../assets/voice/okafor/8f88578b0e56.ogg) |
| L12 | briefing page 2 | Varga | "The pressure readings make no sense. Three CDF weather sondes are still transmitting inside the front.…" | [play](../../../assets/voice/varga/f4f2a71ee4e2.ogg) |
| L12 | t=2 | Okafor | "Weather service calls it the worst Atlantic storm in forty years. It formed in six hours. Stay together." | [play](../../../assets/voice/okafor/bcb16d08c14f.ogg) |
| L12 | t=14 | Rook | "Feel that? The wind's trying to take the stick off me." | [play](../../../assets/voice/rook/431e26a3fe8c.ogg) |
| L12 | t=30 | Varga | "Eel-shaped contacts. They latch on and drain your shield. Shake hard to throw them off." — pinned take (the key seed jumped an octave) | [play](../../../assets/voice/varga/dd97a15d5b15.ogg) |
| L12 | t=44 | Rook | "Seeds on the wind, left side!" | [play](../../../assets/voice/rook/e5c16ad01548.ogg) |
| L12 | t=58 | Varga | "There's a CDF sonde out there. Fly through it and I can read its data." — pinned take; LISTEN: "sonde" | [play](../../../assets/voice/varga/cda9d1e479a9.ogg) |
| L12 | t=86 | Varga | "Lightning shows what's in the clouds. Watch the flashes." | [play](../../../assets/voice/varga/092a1fd001d8.ogg) |
| L12 | t=128 | Okafor | "Storm wall ahead. Instruments will be useless in there. Fly by eye." | [play](../../../assets/voice/okafor/20736f1ea907.ogg) |
| L12 | t=149.5 | Rook | "Blue sky. I'd almost forgotten what it looks like." | [play](../../../assets/voice/rook/6773e26259c2.ogg) |
| L12 | t=157 | Varga | "Lancer, look at them. They're circling with the wind. They aren't caught in this storm. They're steering it." | [play](../../../assets/voice/varga/a7198db19798.ogg) |
| L12 | t=176 | Okafor | "Say again, Doctor?" — pinned take | [play](../../../assets/voice/okafor/602de261bd41.ogg) |
| L12 | t=182.5 | Varga | "They're herding the weather, Commander. They understand our climate better than we do." — LISTEN: "herding" (Whisper hears "hurting" in every seed) | [play](../../../assets/voice/varga/d65128aa69af.ogg) |
| L12 | t=200 | Rook | "Next time, I'm bringing an umbrella." | [play](../../../assets/voice/rook/3b94f40871f1.ogg) |
| L12 | first latch | Rook | "You've got a passenger! Shake it off!" — pinned take | [play](../../../assets/voice/rook/0d795fb668a6.ogg) |
| L12 | level end | Okafor | "You're through. The storm is breaking up behind you, as if someone let it go." | [play](../../../assets/voice/okafor/12cd1dbbab5a.ogg) |
| L12 | secondary met | Varga | "Three sondes, full data. They fed the storm, Commander. I'll have the pattern by morning." — pinned take; LISTEN: "sondes" | [play](../../../assets/voice/varga/c7a82e12794e.ogg) |
| L12 | secret drifting canister | Varga | "A CDF supply canister. Lucky find in this soup." — pinned take (the key seed ended in murmurs) | [play](../../../assets/voice/varga/de6dd3e53a7a.ogg) |
| L13 | briefing page 1 | Okafor | "Lancer, Aegis Actual. Nansen Relay ties every gun on the planet into one defence grid, and a Vrell…" | [play](../../../assets/voice/okafor/bb18f2e538a8.ogg) |
| L13 | briefing page 2 | Varga | "Fast boats weave in the leads: lead your shots or spray. Walkers' claws are armoured in front, so hit…" | [play](../../../assets/voice/varga/3e3034f49873.ogg) |
| L13 | t=1 | Okafor | "Nansen Relay coordinates every gun on the planet. If it goes dark, so does the grid." | [play](../../../assets/voice/okafor/356bd92ff35b.ogg) |
| L13 | t=13 | Varga | "Fast boats in the leads. They weave. Lead your shots, or spray." — pinned take; LISTEN: still a few high syllables (30 % of frames over 300 Hz) | [play](../../../assets/voice/varga/240e8b557207.ogg) |
| L13 | t=24 | Rook | "Cold enough for you, Lancer? My canopy's frosting on the inside." | [play](../../../assets/voice/rook/ec2638fd4fc6.ogg) |
| L13 | t=47.5 | Okafor | "Lancer, you're holding station over the relay. Nothing touches it." | [play](../../../assets/voice/okafor/239a2e047da2.ogg) |
| L13 | t=58.5 | Rook | "Contacts, all sides! Pick a direction, any direction." — pinned take | [play](../../../assets/voice/rook/c2a515c7fc87.ogg) |
| L13 | t=102 | Varga | "Walkers on the floes. Their claws are armoured from the front. Hit them from the side." | [play](../../../assets/voice/varga/4444e5960c1d.ogg) |
| L13 | t=154 | Okafor | "Divers inbound. They're going for the relay, not you." | [play](../../../assets/voice/okafor/1a268a5f0bc9.ogg) |
| L13 | t=172 | Okafor | "Weather's turning. Whiteout coming in from the edges." | [play](../../../assets/voice/okafor/a8d8a6e61f1f.ogg) |
| L13 | t=233.5 | Okafor | "Well held, Aegis. Now we can see the whole board." | [play](../../../assets/voice/okafor/360a71efde31.ogg) |
| L13 | level end | Rook | "Next time they need a relay defended, I'm asking for one with palm trees." | [play](../../../assets/voice/rook/c5016a2df23f.ogg) |
| L13 | mission failed | Okafor | "Nansen is gone. Pull back, Lancer." | [play](../../../assets/voice/okafor/49386929d4bb.ogg) |
| L13 | relay below 25 % | Okafor | "Lancer, the relay is failing. Everything else can wait." | [play](../../../assets/voice/okafor/b6f615cc246d.ogg) |
| L13 | secret research hut cache | Varga | "A CDF research cache. Somebody left that hut in a hurry." — pinned take; LISTEN: "hut" | [play](../../../assets/voice/varga/bc756d169e74.ogg) |
| L14 | briefing page 1 | Okafor | "Lancer, Aegis Actual. Something grew out of Geneva Concord two nights ago: the Siege Spire, three…" | [play](../../../assets/voice/okafor/67dbf1110958.ogg) |
| L14 | briefing page 2 | Varga | "The Vrell fight as one, and I believe the Spire binds them. Its roots run under the old town and grow…" | [play](../../../assets/voice/varga/0e226d3a4ff2.ogg) |
| L14 | t=1 | Okafor | "All units, Aegis Actual. Everything the Coalition has left is in the air over Geneva tonight. Make it count." | [play](../../../assets/voice/okafor/1c2c3e042a52.ogg) |
| L14 | t=13 | Hammer Lead | "Hammer flight on station. Call your targets, Aegis." — pinned take; LISTEN: "flight" | [play](../../../assets/voice/hammer-lead/4a0877190476.ogg) |
| L14 | t=21 | Rook | "Lancer, whatever happens in there, I'm on your wing. As usual." | [play](../../../assets/voice/rook/0e06f6d30260.ogg) |
| L14 | t=31.5 | Varga | "The canopy is one organism. The roots feed the trunk. Kill the roots and the trunk starves." | [play](../../../assets/voice/varga/c7d887d4846e.ogg) |
| L14 | t=54 | Varga | "Hive nodes in the Molard. Hardened, like Lagos." — pinned take; LISTEN: "hive nodes" (raw take still reads "five") | [play](../../../assets/voice/varga/b0a8f0d09999.ogg) |
| L14 | t=62 | Okafor | "Roots surfacing in the streets! They're growing guns." | [play](../../../assets/voice/okafor/fa7514bdb6a8.ogg) |
| L14 | t=102 | Varga | "It's singing to the whole city. Lancer, that's the Spire." — LISTEN: sits higher than her other lines (259 Hz median) | [play](../../../assets/voice/varga/57a6cc0ed4ed.ogg) |
| L14 | Spire destroyed | Varga | "The canopy's dying. All of it, all at once." | [play](../../../assets/voice/varga/c66890f5c69c.ogg) |
| L14 | phase 3 (Free spire) | Okafor | "It's tearing loose! Stay on it, Lancer!" | [play](../../../assets/voice/okafor/c28e5f89ef7f.ogg) |
| L14 | phase 3, after Okafor | Rook | "Roots and all. Somebody call a gardener." | [play](../../../assets/voice/rook/8c39dc4dc9f7.ogg) |
| L14 | phase 2 (Maw) | Varga | "The crown is opening. It's launching flyers, and they'll loop behind you." | [play](../../../assets/voice/varga/35324e4ed247.ogg) |
| L14 | level end | Okafor | "Every Vrell on Earth just lost its voice. They're scattering. We take the fight to them now." | [play](../../../assets/voice/okafor/e295d4babb93.ogg) |
| L14 | secondary met | Okafor | "Root grid in the old town destroyed. The engineers will thank you." — LISTEN: "Root grid" (Whisper hears "route grid") | [play](../../../assets/voice/okafor/dd76b7e52c21.ogg) |
| L14 | secret armoured car | Okafor | "That car's crew left their supplies behind. They'd want you to have them, Lancer." | [play](../../../assets/voice/okafor/282cefb59e48.ogg) |
| Act 2 outro | page 1 | Okafor | "The Siege Spire is dead. Within the hour every Vrell force on Earth lost its coordination. Some are…" | [play](../../../assets/voice/okafor/0164616b057b.ogg) |
| Act 2 outro | page 2 | Okafor | "Nova Lagos is counting its losses. So is Geneva. So are we. Remember the names, then get some sleep." | [play](../../../assets/voice/okafor/9ab0fa49eb2b.ogg) |
| Act 2 outro | page 3 | Okafor | "An hour ago Ares Landing sent a priority signal. Mars is under siege, and the colonies have been holding…" — LISTEN: "Ares" (Whisper hears "air is") | [play](../../../assets/voice/okafor/8daf90352d85.ogg) |
| Act 2 outro | page 4 | Okafor | "We take the fight to them now. Aegis Wing ships out for Mars in seventy-two hours. Aegis Actual out." — pinned take; LISTEN: "ships out for Mars" | [play](../../../assets/voice/okafor/db5a71987932.ogg) |
| L13 | audition clip a | Nansen | Patrick Eaton's LibriVox clip, 124 Hz ([transcript](../../audio/voice/refs/ref-nansen-r35-a.txt)) | [play](../../audio/voice/refs/ref-nansen-r35-a.wav) |
| L13 | audition clip b | Nansen | Sharon Riskedahl's LibriVox clip, 172 Hz ([transcript](../../audio/voice/refs/ref-nansen-r35-b.txt)) | [play](../../audio/voice/refs/ref-nansen-r35-b.wav) |
| L13 | audition, three lines | Nansen | a: Patrick Eaton | [play](../../audio/voice/concept/voice-nansen-r35-a.ogg) |
| L13 | audition, three lines | Nansen | b: Sharon Riskedahl | [play](../../audio/voice/concept/voice-nansen-r35-b.ogg) |
| L12 | sound: thunder a | storm | the bed's own thunder: a big clap and roll, a distant roll, a deep low roll | [1](../../audio/sfx/concept/thunder-1-r34-a.ogg) [2](../../audio/sfx/concept/thunder-2-r34-a.ogg) [3](../../audio/sfx/concept/thunder-3-r34-a.ogg) |
| L12 | sound: thunder b | storm | close strikes: a sharp crack, then the roll, farther each time | [1](../../audio/sfx/concept/thunder-1-r34-b.ogg) [2](../../audio/sfx/concept/thunder-2-r34-b.ogg) [3](../../audio/sfx/concept/thunder-3-r34-b.ogg) |
| L12 | sound: gust a / b | wind | synthesized howl / a recorded gust; does it read as "pushed from the left"? | [a](../../audio/sfx/concept/gust-r34-a.ogg) [b](../../audio/sfx/concept/gust-r34-b.ogg) |
| L12 | sound: latch a / b | Lamprey | suck and bite / a wet splat on a metal thud (not bubbly?) | [a](../../audio/sfx/concept/lamprey-latch-r34-a.ogg) [b](../../audio/sfx/concept/lamprey-latch-r34-b.ogg) |
| L12 | sound: drain loop a / b | Lamprey | a throbbing hum / a siphoning whine (2.4 s loops) | [a](../../audio/sfx/concept/lamprey-drain-r34-a.ogg) [b](../../audio/sfx/concept/lamprey-drain-r34-b.ogg) |
| L12 | sound: shake-off | Lamprey | a wet slap and a squeal whooshing away (+3.5 dB since G8) | [play](../../audio/sfx/concept/lamprey-shake-r34-a.ogg) |
| L12 | sound: sonde chime | sonde | a telemetry chirp into two bell tones | [play](../../audio/sfx/concept/sonde-chime-r34-a.ogg) |
| L12 | sound: rain bed | storm | the rain-only bed (10 s loop): too loud or too thin? | [play](../../audio/sfx/concept/ambience-storm-rain-r34-a.ogg) |
| L13 | sound: relay hit a / b | relay | struck plating and a spark / a thud and a link glitch | [a](../../audio/sfx/concept/relay-hit-r35-a.ogg) [b](../../audio/sfx/concept/relay-hit-r35-b.ogg) |
| L13 | sound: integrity alarm a / b | relay | a hi-lo two-tone / three rising whoops | [a](../../audio/sfx/concept/integrity-alarm-r35-a.ogg) [b](../../audio/sfx/concept/integrity-alarm-r35-b.ogg) |
| L13 | sound: grid online a / b | relay | breakers, a hum and a chord / a spin-up and a clunk (2.6 s, into the jingle) | [a](../../audio/sfx/concept/grid-online-r35-a.ogg) [b](../../audio/sfx/concept/grid-online-r35-b.ogg) |
| L13 | sound: drone launch | drone | four rotors spinning up and climbing away | [play](../../audio/sfx/concept/drone-launch-r35-a.ogg) |
| L13 | sound: relay destroyed | relay | a boom, metal rumbling, a power-down | [play](../../audio/sfx/concept/relay-destroyed-r35-a.ogg) |
| L14 | sound: root burst a / b | root | cracking paving and a crunch / a wet surge and tear (b: a Squelch layer, bubbly?) | [a](../../audio/sfx/concept/root-burst-r36-a.ogg) [b](../../audio/sfx/concept/root-burst-r36-b.ogg) |
| L14 | sound: root growth | growth | a stretching creak and a knock as it sets | [play](../../audio/sfx/concept/root-growth-r36-a.ogg) |
| L14 | sound: tear-free roar a / b | Spire | a tectonic rip and a deep roar / a rising creature bellow | [a](../../audio/sfx/concept/tear-free-roar-r36-a.ogg) [b](../../audio/sfx/concept/tear-free-roar-r36-b.ogg) |
| L14 | sound: canopy withering a / b | canopy | a dry crackling shrivel / a dying exhale and sigh | [a](../../audio/sfx/concept/canopy-withering-r36-a.ogg) [b](../../audio/sfx/concept/canopy-withering-r36-b.ogg) |
| L14 | sound: canopy hum | canopy | the level's ambience, an 8 s loop on 46 Hz (headphones) | [play](../../audio/sfx/concept/canopy-hum-r36-a.ogg) |

## Notes

- The board ([index.html](index.html)) is built from the `r34`, `r35` and `r36` files together
  (`board.py 34 35 36`); each section names its file's round. The videos (`*.mp4`) are links ("open").
  Nansen's reference clips live in `design/audio/voice/refs/`, not in a `concept/` directory, so they
  are only in the *Listening* table.
- The parts waiting for this round: Levels 12–14, the Lamprey, the Skimmer, the Siege Spire, the
  allies (the relay), the HUD (C10) and the Act 2 document (C13) are `design: review`; the parts with an
  open a/b pick (Levels 12–14, the Lamprey, the allies) are `art: proposed`.
- After the choices (at the close):
  - the approved parts get `art: final`; the rejected files move to `concept/rejected/` and their
    generators write the b review there (as round 33);
  - a **b** pick is produced with its generator's variant and `--assets` (the eye: `eye_l12.py` and
    `backdrop_l12.py`; the latch: `lamprey.py`; the relay: `relay.py`; the whiteout: `whiteout_l13.py`
    and `backdrop_l13.py`; the root burst: `root_burst.py`; the daylight: `daylight_l14.py` and
    `backdrop_l14.py`; see each part's `concept/prompts.md`);
  - the accepted sounds go into `sfx_r34.py` / `sfx_r35.py` / `sfx_r36.py`'s production lists and
    `assets/sfx/` with their CREDITS.md rows (the CC-BY ones are already on the credits roll);
  - Nansen: rename the chosen clip to `refs/ref-nansen.wav`, replace `uncast` in the voice data, delete
    the other clip and its CREDITS.md row, render his five lines, empty `AUDITIONING` and rerun
    `RadioTimelineTest`;
  - Levels 12–14 leave `review` once their texts are approved; M5 closes on the roadmap.

## Decisions

- 2026-10-12: Opened as the combined round of M5 parts F–I (the user's request before the vacation:
  one large round at the end), replacing the planned rounds 34–37; `board.py` takes several round
  numbers for it.
