---
title: Concept round 25 — M4 part G, Level 07 and the act end
design: approved
implementation: n/a
art: chosen
depends-on: [../../campaign/act-1-first-contact/level-07-brood-carrier, ../../enemies/bosses/brood-carrier, ../../campaign/act-1-first-contact]
updated: 2026-10-05
---

# Concept round 25 — M4 part G, Level 07 and the act end

## Summary

The production-art round of M4 part G ([roadmap](../../tech/roadmap/README.md#m4-parts)): the
Brood Carrier, its turn, its death and drifting carcass, Level 07's backdrop, the boss and act-end
music (tracks 18, 22 and 24), the two Level 07 briefing images and the four Act 1 outro images,
rendered as **final** art by the generators in [tools/art/](../../../tools/art/README.md) into
`assets/` (straight to production as part F's D8); a/b concepts for the lifeboat tow and the
carrier's and the tow's new sounds; Lifeboat Seven's voice audition; Level 07's lines and the
outro pages voiced; the master limiter; a game capture of the level and the act end; and the
numbers the agents chose to close part G. Open [index.html](index.html) in a browser (regenerate
with `python3 tools/concept/board.py 25`). Every art choice is *approve as final* or *redo* (say
what to change); the concepts, sounds and voices are *pick a or b*; only an approved part gets
`art: final`. Level 07 is playable with `./gradlew :desktop:run --args="--level 7"`; add
`--loadout front=pulse-cannon:3,left=micro-missile-pod:1,right=micro-missile-pod:1` to try the
overhead pass with missiles.

## Choices

| # | Part | What is proposed | Review files | Weak spots (Claude) | Outcome |
|---|---|---|---|---|---|
| 1 | [Brood Carrier](../../enemies/bosses/brood-carrier/README.md) | production art of the chosen r04 a model re-laid to the data (`tools/art/brood_carrier.py`): nine hull frames (nose-down 288×626, 7 turn frames 11.25° apart with the head swinging right, broadside 626×288), so the turn of D1 is never rotated at run time; the bay sac nose-down and broadside (closed, 2 swelling, open, burst; 40×40), the plate iris (5 stages, 64×64), the core glow, the mandible turret at 17 headings; one 48-colour palette; the data's hit boxes and part offsets fit both poses unchanged | `brood-carrier-final-r25-a` (png, gif), A1's capture `brood-carrier-capture-final-r25-a` | the turn frames always show closed sacs, so a sac burst in phase 1 looks intact again during the 2 s turn; the high-air shadow is hard to see on black space; A1's capture predates the 75 % translucency (3) and the fixes; Level 07's unit atlas is one 2048² page, 65 % full, the carrier alone | **approved as final** (user); the closed sacs in the turn frames and the faint high-air shadow accepted as they are |
| 2 | [Carrier death and carcass](../../enemies/bosses/brood-carrier/README.md#concept-art) | production art (`tools/art/brood_carrier_death.py`): a break-up from the start, eight chunks swapped in under the flash at the end of the 3 s tail-to-head chain, each with 3 tumble frames, a wave of blasts running with the chain, the ichor cloud; after the capture's fixes the carcass has no lights of its own, its cuts cool over the tumble frames, the blasts trailing on the chunks are fire only, and the chunks drift down and to the right for the whole 35 s aftermath | `brood-carrier-death-final-r25-a` (png, gif) | just before the swap the core glows dim lime while the chunks show it torn (hidden by the flash); the ichor is a dark teal; the gif's last part is a 10× time-lapse, the real drift is slow | **approved as final** (user), the dim core glow before the swap and the dark teal ichor as they are |
| 3 | [Carrier on high air at 75 %](../../enemies/bosses/brood-carrier/README.md#phases) | the user's decision after the first capture, built: on `high-air` the hull is drawn at 75 % opacity like the Leviathan, easing to opaque as it descends, so the ship and its shots show through it; the units it launches are drawn over the hull; a hit sac or core takes a held white tint instead of a white strobe. **Question:** the ship under the hull reads only faintly, at about 25 % contrast. Is that enough, or should the hull be more see-through? | `level-07-capture-final-r25-a` (png, mp4: the overhead phase) | the opacity is the shared high-air value of the set pieces, so a change applies to the Leviathan too unless the carrier gets its own | **75 % kept as it is** (user): the shared high-air value of the set pieces, the Leviathan unchanged |
| 4 | [Level 07 backdrop](../../campaign/act-1-first-contact/level-07-brood-carrier/README.md#layout) | production art (`tools/art/backdrop_l07.py`, its block merged into the level's data): stars, Earth's whole disc and the Moon on deep, the sister sensor station breaking up and the picket's torn ring on far, the carrier's hazed silhouette ahead (sections 2–3, on a path) and the second fleet's glittering line (aftermath), overrun picket platforms, torn gun mounts and truss, wrecks, Level 03's spore haze and banks and the aftermath's ichor clouds on low-air, frost and spore streaks on high-air; one 2048² page, 76 % full | `backdrop-final-r25-a` | the deep factor is **0.015** (the art direction says 0.05–0.2), so Earth and the Moon stay on screen from the start to the aftermath; the far silhouette cannot grow and points nose-up, while the real carrier comes over nose-down; Earth's disc is large at the start; Level 03's spore banks are bright over black; the overgrown platforms keep Level 02's violet and teal; the fleet line is faint and crosses Earth's limb late in the aftermath | **approved as final as it is** (user), the deep factor 0.015 kept |
| 5 | [Lifeboat tow](../../campaign/act-1-first-contact/level-07-brood-carrier/README.md#secrets-and-pickups) | **concepts**, pick a or b (`tools/concept/props_r25.py`): a *white rescue boat* (a white lifting-body lifeboat 72×36 with orange outer wings, a CDF blue band, lit windows and a green strobe; a plain olive canister pod 32×32; a braided tether 16×44 whose three amber marker lamps are the only target cue, one going out per hit) or b *orange lifeboat capsule* (a rescue-orange pressure capsule with white end caps, portholes and two blue strobes; a crate-pod with an amber and black hazard lid that reads as loot; a thin cable with an amber light strip and a striped breakaway coupler, split by the second hit) | `lifeboat-r25-a`, `-b` (png, gif) | the sheets show the crate falling free at the cut; since the user's pick the pod comes loose, tumbles away and the crate falls out of it low on the screen, which the chosen variant's production art must show; production needs the game's TowLooks frames: `lifeboat` 2 frames (strobe lit and dark), `lifeboat-cable` 4 frames by hits taken, the glows; the capture still shows placeholder sprites | **b** (user): the orange lifeboat capsule, the hazard-lidded crate-pod and the amber cable with its breakaway coupler; a rejected; b's production sprites and TowLooks frames follow |
| 6 | [Carrier and tow sounds](../../audio/sfx/README.md#concept-art) | **recorded** (CC0 / CC-BY, `tools/concept/audio/sfx_r25.py`, cut from the Freesound originals), pick a or b per sound: **s1 roar** at the arrival and, lower, at the turn (a deep roar with echo, 4.0 s; b bear over a didgeridoo, 3.0 s); **s2 sac open / close** (a flesh pulled apart, KVV; b a tear out of sucking mud, its close reversed, LucasDuff); **s3 launch** (a creature spit; b slime lunge); **s4 sac burst** (a very wet, fleshy explosion; b a visceral tear with a wet tail); **s5 iris** (a alien hatch; b grinding organic morph); **s6 cable snap** (a chain snap with a rattle; b a slowed string snap) | `enemy-carrier-roar-r25-a`, `-b`, `enemy-carrier-sac-open-r25-a`, `-b`, `enemy-carrier-sac-close-r25-a`, `-b`, `enemy-carrier-launch-r25-a`, `-b`, `enemy-carrier-sac-burst-r25-a`, `-b`, `enemy-carrier-iris-r25-a`, `-b`, `secret-cable-snap-r25-a`, `-b` | picked by description, rating, envelope and level, not listened to; option a plays in the game for now; **the 4 s roar a overlaps the klaxon** (confirmed in the capture: it starts 0.75 s after the klaxon); the sacs open and close about **every 1.2 s** in the broadside windows, which may be clutter; CC-BY (credits screen): s2 a, s4 a, s6 a (CC-BY 3.0) and s6 b (CC-BY 4.0), so the cable snap is CC-BY either way; round 08's `enemy-spawn-r08-a` could be a c for the launch | **roar b, sac open b, sac close b, launch b, sac burst a, iris b, cable snap a** (user); the others moved to `concept/rejected/`; the game plays the chosen ones, written to `assets/sfx/` by `tools/art/sfx_originals.py` with a `SOURCE` comment; the sac burst a and the cable snap a are CC-BY (credits screen) |
| 7 | [Lifeboat Seven voice](../../audio/voice/README.md) | **audition**, pick a or b: the t=22 line ("Lifeboat Seven, crew of four, drifting. Aegis, that pod we're towing is yours if you cut it loose.") through Chatterbox and filter b at the neutral settings (`tools/concept/audio/tts_r25.py`), a: Tadhg Hynes, b: Lizzie Driver (LibriVox, public domain) | `voice-lifeboat-seven-r25-a`, `-b` (clips `ref-lifeboat-seven-r25-a`, `-b`) | Whisper hears "Aegis" as "eegis" (a) and "ages" (b), and b's filtered take once as "telling" for "towing"; until the choice the speaker is `uncast` and the line plays as text | **a, Tadhg Hynes** (user): cast (`ref-lifeboat-seven.wav`, `uncast` dropped), the t=22 line rendered (6.5 s; Whisper reads it back word for word, "Aegis" included); b moved to `concept/rejected/`, its clip deleted |
| 8 | [Tracks 18, 22 and 24](../../audio/music/README.md#concept-art) | production files (`tools/art/themes.py`), audio identical to the chosen round-08 concepts with a `SOURCE` comment: #18 "The Choir Descends" (125.2 s, looped), #22 "Red Alert" (5.30 s, once; track 18 comes in at 4.8 s, three bars at 150 BPM), #24 "Act Complete" (16.33 s, once, under outro page 1); with two listening aids, the 22 → 18 hand-off as the game mixes it and track 18 across its loop seam | `choir-descends-final-r25-a`, `boss-warning-final-r25-a`, `act-complete-final-r25-a`, `boss-handoff-final-r25-a`, `choir-descends-seam-final-r25-a`, `boss-music-final-r25-a` (png) | listen to the hand-off: it drops 3.3 LU (the warning's last bar −11.6 LUFS, track 18's first −14.9 LUFS) into the boss theme's quieter intro; the seam jump measures 0.35 of the nearby sample-to-sample change; no stems for the boss theme | **approved as final** (user), the hand-off's 3.3 LU drop and the seam as they are |
| 9 | [Level 07 briefing and Act 1 outro images](../../ui/briefing/README.md) | production art (`tools/art/briefing_images.py`, straight to production per D8; D3: one per outro page): `level-07-l1-carrier` (Okafor: the carrier holding at L1 with its escort screen, the pods on Luna traced back to it), `level-07-overhead-scan` (Varga: nose-down on high air over the ship, only missiles reaching up, the turn, broadside with the iris open), `act-1-outro-carcass`, `act-1-outro-daedalus-rim`, `act-1-outro-second-fleet`, `act-1-outro-rook` | `briefing-images-final-r25-a` | rendered before the carcass fix: the head chunk's eye lights still glow in the carcass image (re-render after this round); Rook's craft is the round-09 concept render (no production wingman sprite yet); the open sacs read lime only through an added glow (the game's open sac is a muted pale green); the overhead scan is busy (3 panels, 197 colours); the Daedalus Rim image says "MAIN AIRLOCK - OPEN" while Level 06's gate sprite looks shut | **approved as final as they are** (user); the carcass image keeps the head chunk's eye lights (a re-render with the current carcass moved the chunks over the header, so it waits for a later round) |
| 10 | [Level 07 lines and outro pages voiced](../../audio/voice/README.md) | Level 07's 18 lines (the two briefing pages, 15 radio lines, the lifeboat tow secret; Okafor 6, Varga 8, Rook 4) and the Act 1 outro's four pages (dry, Okafor 3, Varga 1), rendered by `tools/art/voice.py` with the cast voices; listen under [Listening](#listening) | the files in `assets/voice/` (see [Listening](#listening)) | listen for **"Daedalus" on outro page 2**, which may be stressed oddly; Rook's "Okay. Okay. That was big. We did big." repeated its last sentence with the key's seed: the take on seed 267639418 is **pinned** in the speaker table; Okafor's "Every bay gutted…" passed on its second seed; Whisper reads every take back (Aegis and Daedalus only with a name prompt); outro page 1 is 14.3 s, under track 24's 16.3 s; Lifeboat Seven's line waits for 7 | **accepted** as rendered (user), the pinned take of Rook's and "Daedalus" on outro page 2 as they are |
| 11 | [Master limiter](../../audio/README.md#master-limiter) | approve: the audio library's own output limiter on the final mix holds every peak at full scale (the capture's mix peaked at 1.2–1.46, Level 05's at 1.59, clipped by the sound card); 1 ms look-ahead, acting only at full scale, so until a level's first over the mix passes sample for sample | — (heard in the capture's mp4) | inaudible by measurement except a slow make-up release after an over: about −0.35 dB half a second later, −0.15 dB after 2 s, under −0.05 dB after 4–5 s (about −1 dB just after a loud over), on average 0.1 dB lower through a fight; not judged by ear | **approved** (user) |
| 12 | [Level 07 in the game](../../campaign/act-1-first-contact/level-07-brood-carrier/README.md) | a game capture with `--level 7 --invulnerable` and a homing fit: the lifeboat tow and its cut, the boss warning, the overhead pass, the turn, broadside, the iris and core, the death chain, the aftermath, the debrief, the act summary and the four outro pages | `level-07-capture-final-r25-a` (png, mp4 with the game's sound) | re-recorded after the fixes; see the capture's [prompts.md entry](../../campaign/act-1-first-contact/level-07-brood-carrier/concept/prompts.md#level-07-capture-final-r25-a) | **accepted** (user) |
| 13 | [Part G numbers chosen by the agents](#part-g-numbers) | accept or change the numbers and defaults listed under [Part G numbers](#part-g-numbers), and check the user decisions listed there were carried out | — | after the homing fix the autopilot kills at 82.8–90.2 s, under the par of 100 s, so a run like the autopilot's earns the Boss rush bonus | **accepted as listed** (user); the user decisions of part G carried out |

## Part G numbers

Chosen by the agents to close part G's doc gaps (choice 13); the details live in the parts'
READMEs (Decisions of 2026-10-05) and the data files of
[Level 07](../../campaign/act-1-first-contact/level-07-brood-carrier/data.yaml) and the
[Brood Carrier](../../enemies/bosses/brood-carrier/data.yaml).

- **Density**: the waves densified to 105 units (129 with the pods' Skitters), about **81 a
  minute**, the draft's beats at their times, two Skitter streams at t=26 and t=70.5 closing the
  pacing gaps.
- **Budget**: `bounty_scale` **0.75**, typical haul **1,049** of the budget's 1,051 (−0.2 %),
  perfect **1,542**; the carrier keeps its 450 bounty (bays 25 each, core 250), 340 at the scale.
- **Par**: **100 s** (user, tightened from 150 s): the autopilot's runs took 72 / 90 / 102 s; the
  fight length and the core's HP stay.
- **Boss spawns**: a typical fight (the autopilot with the plan's fit at medium) launches 41
  Skitters and 8 Needlers (phase 1: 16 + 8, phase 2: 25), which the credit budget counts; easy:
  phase-1 openings spawn 3 Skitters or 1 Needler; hard: every opening that spawns Skitters spawns
  1 more, the phase-3 spiral has 4 arms.
- **Boss timing**: broadside station x 170, y 150; phase-2 windows open 0.2 s after each volley,
  phase-3 windows (after a timeout) 3 s after the iris; the timeout's 70 s run from the end of the
  turn; the spiral 8 bullets/s over 3 arms for the whole phase, the ring at 110 px/s; the iris
  takes 1 s to open; one head turret (`mandibles`) instead of two, fire only.
- **Phase-1 homing after the fix**: against a high-air boss a missile seeks its open parts all
  round within 350 px, climbs to its target at twice its turn rate and passes beneath the hull
  (homing elsewhere unchanged). The autopilot at medium (pulse-cannon:3) does 354 HP to the
  carrier in phase 1 with pods L1 (no sac burst; easy 448, hard 198) and 884 HP with pods L4 (one
  sac burst, the overdrive drop); the bays secondary is met in every run; the kill comes at 82.8 s
  with L1 pods (90.2 s with the plan's fit).
- **Lifeboat tow**: t=22 from x 130, drifting (12, −45) px/s; boat 72×36, pod 32×32 70 px behind,
  cable 16×40, 3 hits; the secondary's tracker label `BAYS`, 50 credits, failed when the
  `Broadside` phase times out ("BONUS: DESTROY ALL 8 BAYS BEFORE THE BROADSIDE ENDS").
- **Radio retimes**: every timed line starts within 1 s of the rule; Varga's carrier line, shortened
  to two pages, is spoken at **t=82.5** (at t=84 it ran 1.5 s into the Choir's t=94 and pushed
  Okafor's t=100 and Varga's t=107).
- **Shortened intel texts** (the hangar's intel panel ran 4 px past its foot at sensor L2):
  the teaser reads "One big target, armoured sacs in a row. Something that punches through them
  all, and missiles, if you can afford them."; Varga's intel line is "One ship at L1.
  Station-sized."; the OBJECTIVE field reads `DESTROY THE CARRIER` (the full name runs past the
  panel's right edge).
- **Rook's new secret line** (the lifeboat tow's, given to Rook): "Pod's loose. Lifeboat Seven says
  it's yours, Lancer. Finders keepers."
- **Act summary layout** ([debrief](../../ui/debrief/README.md)): a second page after the last
  level's tally, a row per won level with its banked credits (with the grade bonus), kills and best
  grade, the act's data cores with their lore titles; the save records them from format version 2
  on (older saves show what is recorded); the BOSS TIME row, missing since part E, built with it.
- **Defaults of the D-decisions** (stated by the main agent, not objected to): broadside held about
  70 px off-centre with the head inside the field; in phase 1 only open sacs take damage, only from
  `homing`; phase-2 windows release 1 Skitter per open living sac and cycle through the living
  pairs; a timeout spawns 2 Skitters per surviving pair every 6 s; surviving sacs burst and pay at
  the kill; the approach densified with the act's light enemies (as Level 06's D1); the closing
  lines are boss-destroyed cues in queue order; the boss-destroyed cue bug fixed; a/b concepts only
  for the lifeboat tow and the new sounds, the carrier, backdrop and briefing images straight to
  production; Lifeboat Seven uncast until this round; per-level earned credits and kills recorded
  for the act summary; the `space` layer and Bomb Rack "NO GROUND" items retagged.

**User decisions of part G** (check they were carried out):

- **D1 = b**: the carrier comes over nose-first at 40 px/s, stops above the ship and spawns, then
  descends and turns 90° in place through pre-rendered frames, invulnerable (choice 1).
- **D2 = a**: a 35 s aftermath with the drifting carcass and the closing lines in flight, then the
  debrief with the act summary, the act outro (four voiced pages, track 24 under page 1) and the
  hangar before Level 08 (choice 12).
- **D3 = b**: four outro images, one per page (choice 9).
- **D4 = b**: the Decoy Flares leave Level 07; confirmed: they unlock at **L27** (the first homing
  projectile; the Lamprey's chase at L12 does not count).
- **Phase-1 timing**: bay openings every 2.5 s from about 6 s; a pair with one dead sac spawns
  half, rounded up.
- **No act summary and no outro on a replay** (a replay ends with a normal debrief).
- **Par 100 s** instead of 150 s.
- **75 % translucency** on high air, like the Leviathan (choice 3).
- **The lifeboat's pod after the cut**: it comes loose and tumbles away from the boat, and the
  crate falls out of it low on the screen, a normal hidden crate from there (choice 5).

## Listening

Level 07's lines and the Act 1 outro pages as the game plays them (choice 10), from `assets/voice/`.

| Line | Speaker | Text | Take |
|---|---|---|---|
| Briefing page 1 | Okafor | "Aegis, Varga has traced every pod that hit Luna back to one ship…" | [play](../../../assets/voice/okafor/d8e48d575f6d.ogg) |
| Briefing page 2 | Varga | "On its first pass it flies above you, and only missiles reach that high…" | [play](../../../assets/voice/varga/dfb8d4eb4990.ogg) |
| t=1 | Okafor | "This is it, Aegis. Kill the carrier and the pods stop coming." | [play](../../../assets/voice/okafor/f98af4eea4f3.ogg) |
| t=8 | Rook | "Aegis Two and the rest of the wing are on its far flank…" | [play](../../../assets/voice/rook/3961e9a14f67.ogg) |
| t=44 | Varga | "Spore bombers screening the carrier. Same rules as the high lanes." | [play](../../../assets/voice/varga/bc41283491da.ogg) |
| t=72 | Rook | "Edges, Lancer! Snipers, both sides!" | [play](../../../assets/voice/rook/f7a40736c564.ogg) |
| t=82.5 | Varga | "Carrier in visual range. Everything it launches comes through those sacs. Hit the lime glow." | [play](../../../assets/voice/varga/0ae061ec3c2a.ogg) |
| t=100 | Okafor | "Brood Carrier, dead ahead. She's all yours, Lancer." | [play](../../../assets/voice/okafor/fd45aff01f33.ogg) |
| t=107, homing | Varga | "It's right over you. Your missiles can reach the open sacs…" | [play](../../../assets/voice/varga/ac0cec6cfd05.ogg) |
| t=107, no homing | Varga | "It's above you. You can't touch it up there…" | [play](../../../assets/voice/varga/29aed32f9537.ogg) |
| Phase 2 | Varga | "It's coming down and turning broadside!…" | [play](../../../assets/voice/varga/352e5d20d1fa.ogg) |
| Phase 2 timeout | Okafor | "Forget the sacs. Go for the core." | [play](../../../assets/voice/okafor/59b4edf1f705.ogg) |
| Phase 3 | Varga | "The iris is open. That's the core. Everything you've got!" | [play](../../../assets/voice/varga/dbe3823986a8.ogg) |
| Carrier destroyed 1 | Rook | "Okay. Okay. That was big. We did big." (pinned take) | [play](../../../assets/voice/rook/ff3da796bbb8.ogg) |
| Carrier destroyed 3 | Varga | "Same pattern as at the shipyards, and a new one…" | [play](../../../assets/voice/varga/919ca07ff399.ogg) |
| Carrier destroyed 4 | Okafor | "Long range just lit up. A second fleet, much bigger, heading for Earth…" | [play](../../../assets/voice/okafor/df895b8df9bf.ogg) |
| Secondary met | Okafor | "Every bay gutted. That carrier launches nothing more. Textbook, Lancer." | [play](../../../assets/voice/okafor/68c2e982bd56.ogg) |
| Lifeboat tow secret | Rook | "Pod's loose. Lifeboat Seven says it's yours, Lancer. Finders keepers." | [play](../../../assets/voice/rook/413c5f0d92e8.ogg) |
| Outro page 1 | Okafor | "The carrier is dead…" (14.3 s, under track 24) | [play](../../../assets/voice/okafor/5b2dc67f19cd.ogg) |
| Outro page 2 | Okafor | "The farside settlements are still empty. Daedalus Rim and two others…" (listen for "Daedalus") | [play](../../../assets/voice/okafor/18243a40916d.ogg) |
| Outro page 3 | Varga | "The fleet on long range is ten times the size of the carrier's group…" | [play](../../../assets/voice/varga/e70d9dde3770.ogg) |
| Outro page 4 | Okafor | "Then we meet them on the ground…" | [play](../../../assets/voice/okafor/35a4cb09fe47.ogg) |

## Notes

- Level 07's unit atlas is one 2048² page (65 %, the carrier alone); its backdrop is one 2048²
  page (76 %).
- Review file names: `<subject>-final-r25-a.png/.gif/.ogg` (production art and music) and
  `<subject>-r25-a/b.*` (concepts, sounds, voices) in each part's `concept/`; their `prompts.md`
  entries say how they are made. Nothing in `assets/` is hand-edited.
- After the choices: the rejected variants move to their `concept/rejected/` (CREDITS.md rows
  updated); the chosen lifeboat gets its production sprites and TowLooks frames; the chosen
  sounds replace option a in `Sfx`; Lifeboat Seven's reference is renamed, `uncast` dropped and its
  line rendered; the carcass outro image is re-rendered.

## Decisions

- 2026-10-05: Opened with M4 part G.
- 2026-10-05: Closed (user: lifeboat tow b; carrier iris b, launch b, roar b, sac burst a, sac
  close b, sac open b; cable snap a; Lifeboat Seven a; all other rows accepted). Approved as final:
  the Brood Carrier's sprites and turn, its death and drifting carcass, the Level 07 backdrop,
  tracks 18, 22 and 24, the Level 07 briefing and Act 1 outro images; kept as it is: the 75 %
  high-air opacity; accepted: the voiced lines, the master limiter, the capture, the part G numbers
  and the decision checklist. The rejected sounds and Lizzie Driver's take moved to their
  `concept/rejected/` (CREDITS.md rows follow), her reference clip deleted with its row; the chosen
  sounds play in the game (`Sfx`, `copyPlaceholderSounds`); Lifeboat Seven cast and its line
  rendered. The lifeboat's production sprites from b are a separate step. Left for later: the
  carcass outro image's re-render. The Choices rows had an extra empty cell; fixed with the outcomes.
