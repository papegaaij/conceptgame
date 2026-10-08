---
title: Concept round 32 — M5 part D, Level 10 Evacuation Corridor
design: approved
implementation: n/a
art: chosen
depends-on: [../../campaign/act-2-homefront/level-10-evacuation-corridor, ../../enemies/air/wraith, ../../enemies/air/mote-swarm, ../../allies, ../../ui/briefing, ../../ui/hangar, ../../ui/hud, ../../audio/sfx, ../../audio/voice, ../../player/wingmen, ../../art-direction/production]
updated: 2026-10-08
---

# Concept round 32 — M5 part D, Level 10 Evacuation Corridor

**Closed 2026-10-08** (user): the production art approved as final (the Wraith, the Mote Swarm,
their intel portraits, Level 10's backdrop with the ferry hatch, the two briefing images); the
evacuation shuttle **a**, the scripted loss's look **b** "Iris column", the decloak sound **b**
(CC-BY, F.M.Audio on the credits roll), the Mote Swarm sound **b**, the lance sound **a**, the
Lifeline voice **b** and the Lifeline Three voice **b**; the captures, voiced lines, texts (Level 10
now `approved`), numbers, decisions and every build choice accepted as built. The picks are in the
game (the two voices cast and their eight lines rendered, Lifeline Three's cut off into static); the outcome of each row is in the *Choices*
table.

## Summary

The review round of M5 part D ([roadmap](../../tech/roadmap/README.md#m5-parts)): Level 10
*Evacuation Corridor*, the act's `escort` level and the first rear-heavy one. The production art
went straight to production (D10 = a) and is reviewed here as **final**:
- the Wraith, with its cloak shimmer, decloak flash, death and intel portrait;
- the Mote Swarm, with its flicker, ember puff, flock loop and intel portrait;
- Level 10's backdrop at first light (D11 = a), with the capsized ferry's hatch;
- the two briefing images.

Seven a/b picks are new: the evacuation shuttle, the scripted loss's look (the glow and the lance),
three sounds (the Wraith's decloak, the Mote Swarm, the lance's strike) and two voices (Lifeline and
Lifeline Three). The round also holds:
- the game captures of Level 10, with the loss capture and the readability strip;
- a listen to the 22 new voiced lines;
- the new texts (Level 10's document is `design: review`), quoted in full;
- the numbers the agents chose or measured;
- the user's part D decisions, for the record;
- the choices made during the build, to confirm one by one.

Open [index.html](index.html) in a browser (regenerate with `python3 tools/concept/board.py 32`).

Every art row is *approve as final* or *redo* (say what to change). An a/b row is *pick a or b*.
A listening, text or numbers row is *accept* or *change* (say what). The build-choice rows list
lettered items, each *yes* (keep as built) or *no* (say what to change). Only an approved part gets
`art: final`.

In the game (`./gradlew :desktop:run --args="…"`):
- Level 10 with the capture's fit (the balance plan's L10 fit: the Tail Gun bought, Rook's
  Autocannon refitted): `--level 10 --escort rook:autocannon:2 --loadout
  front=pulse-cannon:3,left=bomb-rack,right=autocannon-pod,rear=tail-gun:1 --special airstrike:2`.
  Since the close the game flies shuttle **a**, draws loss look **b** and plays the picked sounds
  (decloak b, swarm b, lance a from t 116.8); until the round's captures were taken it drew loss
  look a and played placeholder sounds (the decloak: the Mantis telegraph a; the swarm: the sled
  pass b; the lance: hit-crumble a) and showed the Lifeline lines as text.
- The fail rule: the same on hard without a rear gun and without guarding the shuttles
  (`--difficulty hard --escort none --loadout front=pulse-cannon:3`): losing the four saveable
  shuttles fails the mission at once, with Okafor's line on the mission failed screen
  (`Level10Test` checks it on hard, the four lost by t≈114).
- The way in: `--level 9 --invulnerable`. Winning it plays the debrief, Level 10's two briefing
  pages and the hangar with Rook's teaser, the intel panel (at each sensor level) and, launching
  without a rear weapon, the `NO REAR WEAPON FITTED` warning.

## Choices

| # | Part | What is proposed | Review files | Weak spots (Claude) | Outcome |
|---|---|---|---|---|---|
| 1 | [Wraith](../../enemies/air/wraith/README.md#concept-art) | production art (`tools/art/wraith.py`, straight from the chosen round-06 model, no a/b): `wraith_0..63` (72×72, 16 headings × 4 wing-ripple frames; a 1 px lavender-white rim at 0.8 for the dawn); the **cloak** shimmer `wraith-cloak_0..63` (90×90, additive, at the high-air scale: a broken lavender glint round the silhouette, faint refraction bands, the veins and eyes as a 28 % ghost; drawn instead of the body, no shadow); the 0.4 s **decloak** flash `wraith-decloak_0..5` (112×112); the `medium` **death** and membrane **tatters** (96×96). The blue-violet **veins** are emissive in the frame and drawn only (their ×1.5 struck, a part D default). Look at the cloak over the city and over the water, the decloak sequence, the veins and the death | [wraith-final-r32-a.png](../../enemies/air/wraith/concept/wraith-final-r32-a.png), [.gif](../../enemies/air/wraith/concept/wraith-final-r32-a.gif) | the decloak flash's **frame 1 looks blobby**; the tatters read as sticks; the sheet shows the shimmer on a dawn stand-in (Level 08's avenues), not Level 10's backdrop, and in the game the cloak is faint over the city (readability strip, row 14); the review GIF holds the Wraith facing up and fires the fixed fan straight up, as the game does || **approved as final** (user): `wraith_0..63`, the cloak shimmer, the decloak flash, the `medium` death with its tatters and the intel portrait; the weak spots as they are (21d, the blobby frame 1, kept); Wraith `art: final`, `done`, its numbers and readings accepted (rows 17, 19), so `design: approved` |
| 2 | [Mote Swarm](../../enemies/air/mote-swarm/README.md#concept-art) | production art (`tools/art/mote_swarm.py`, from the chosen round-05 model): `mote-swarm_0..47` (16×16, 16 headings × 3 glow-flicker frames, one bone spike leading so a flock reads aligned, a faint rim); the `tiny` **ember puff** `mote-swarm-death_0..7` and tatters (24×24). The GIF flies a 20-mote boids flock that loops back (the review's own simulation) | [mote-swarm-final-r32-a.png](../../enemies/air/mote-swarm/concept/mote-swarm-final-r32-a.png), [.gif](../../enemies/air/mote-swarm/concept/mote-swarm-final-r32-a.gif) | the ember puff is **muddy**; the GIF's flock is not the game's (the game's tuning is confirmation 19e) || **approved as final** (user): `mote-swarm_0..47`, the ember puff with its tatters and the intel portrait; the weak spots as they are; Mote Swarm `art: final`, `done`, its numbers and readings accepted (rows 17, 19), so `design: approved` |
| 3 | [Intel portraits](../../ui/hangar/README.md#concept-art) | production art (`tools/art/intel.py`): the 30×30 sensor-L2 portraits of the Wraith and the Mote Swarm from their production models, at 1× and 3× | [intel-final-r32-a.png](../../ui/hangar/concept/intel-final-r32-a.png) | || **approved as final** (user) |
| 4 | [Level 10 backdrop](../../campaign/act-2-homefront/level-10-evacuation-corridor/README.md#layout) | production art (`tools/art/backdrop_l10.py` on Level 08's kit with Level 09's creep helpers, its block merged into the level's data, 439 placements, 3 of 6 atlas pages): the outskirts at **first light** (D11 = a). Section 1 Eko spaceport with the five launch pads; 2 the suburbs with the jammed motorway, refugee lights, parks and pitches and CDF gunships flying the other way; 3 the maglev viaduct between two creep districts, fires and smoke plumes; 4 the coast road, piers, sandbars and the **capsized ferry**; 5 the open lagoon with buoys under the morning sky. Haze, smoke and sea mist drift west on the sea wind. The GIF plays the liftoff (t 0–8) with shuttle a | [backdrop-final-r32-a.png](../../campaign/act-2-homefront/level-10-evacuation-corridor/concept/backdrop-final-r32-a.png), [.gif](../../campaign/act-2-homefront/level-10-evacuation-corridor/concept/backdrop-final-r32-a.gif) (the generator's block: [backdrop-proposal.yaml](../../campaign/act-2-homefront/level-10-evacuation-corridor/concept/backdrop-proposal.yaml)) | sections 1–3 still read **night-navy** more than dawn (confirmation 21a); the gunships have **no rotor** (21b); the water is flat; 330 towers || **approved as final** (user): the weak spots as they are (21a night-navy and 21b the gunships without rotor kept); Level 10 `art: final` |
| 5 | [Ferry hatch](../../campaign/act-2-homefront/level-10-evacuation-corridor/README.md#secrets-and-pickups) | production art (`backdrop_l10.py --props`, on the backdrop's sheet): `ferry-hatch_0..2` (36×32: shut with its rim, hand wheel, six dogs and amber lamp; dented and torn; open, the door blown aside and the crate in the dark hold) and the six-piece break `ferry-hatch-break_0..7`, as Level 09's cocoon | the hatch frames on [backdrop-final-r32-a.png](../../campaign/act-2-homefront/level-10-evacuation-corridor/concept/backdrop-final-r32-a.png); in the game: [level-10-readability-r32-a.png](../../campaign/act-2-homefront/level-10-evacuation-corridor/concept/level-10-readability-r32-a.png) | the lamp does not blink (static); the **crate is barely legible** in the open hold (confirmation 21c); the hatch has 50 HP since the fix pass (row 17) || **approved as final** (user): the weak spots as they are (21c kept); the hatch's 50 HP accepted (row 17) |
| 6 | [Level 10 briefing images](../../ui/briefing/README.md#concept-art) | production art (`tools/art/briefing_images.py`, 672×240, one per page). **Okafor's page** `level-10-evacuation-route`: the corridor at first light from Eko spaceport's five pads (shuttle a at ½, numbered One to Five) over the suburbs, the viaduct and the coast road with the ferry to the lagoon's climb-out, the route dashed amber, Lancer and Rook, red contacts closing from behind. **Varga's page** `level-10-wraith-scan`: the Wraith cloaked and decloaked, "only homing finds it", its pass overhead, loop and rear entry with the 3 s warning and bursts up through the shuttle band; a Mote Swarm's loop-back | [briefing-images-final-r32-a.png](../../ui/briefing/concept/briefing-images-final-r32-a.png) | the shuttles on the route image are variant a (redrawn if b is picked, row 7) || **approved as final** (user): both images; shuttle a picked (row 7), so the route image needs no redraw |
| 7 | [Evacuation shuttle](../../allies/README.md#evacuation-shuttle) | **pick a or b**, both at production quality (`tools/art/shuttle.py`, 64×40, always nose up): five bank frames (−30…+30°), a damaged set (scorched, one engine dead, smoke below 50 %), four liftoff steps (0.70–0.925×), eight glide-wreck frames, engine flames and climb-out flare, the HUD pip. **a** "lifting body": a broad white blended wing, dark heat-shield nose, cabin windows down the spine, an orange evac chevron, three engines. **b** "heavy lifter": a long white cabin fuselage with two window rows, stub wings with ducted lift fans and orange-nosed nacelles, a T-tail, two engines. The GIFs: three shuttles lift off, drift, one is hit, smokes, is lost and glides away, the others climb out | [evacuation-shuttle-r32-a.png](../../allies/concept/evacuation-shuttle-r32-a.png), [.gif](../../allies/concept/evacuation-shuttle-r32-a.gif); [evacuation-shuttle-r32-b.png](../../allies/concept/rejected/evacuation-shuttle-r32-b.png), [.gif](../../allies/concept/rejected/evacuation-shuttle-r32-b.gif) | a is in the game (b only in its review files until picked: `shuttle.py --variant b` writes it); the bank is **subtle** (the lane sway reaches about ±1 frame); the scorches are small; the wreck is hazy || **a** (user), the lifting body, already in the game: approved as final, the weak spots as they are; b moved to `concept/rejected/` |
| 8 | [The scripted loss's look](../../campaign/act-2-homefront/level-10-evacuation-corridor/README.md#concept-art) | **pick a or b**, both at production quality (`tools/art/lance_l10.py`). **a** "Thorn spear": a violet light swells in the cloud ahead of Lifeline Three (72 px above it), pulsing quicker for 2 s; at 118 a thin violet spear with backward thorns drops onto the shuttle, a ring flash with eight thorn spikes, the spear breaks into glowing motes. **b** "Iris column" (`lance_l10.py --variant b`): a teal vortex opens straight above Lifeline Three, framing it in a violet ring; at 118 a broad teal-white column lands on it and collapses into rippling teal and violet shock rings. Both: the glow from t=116 drawn under the air layer, the lance above it, the wreck's glide into `far` | [loss-r32-a.png](../../campaign/act-2-homefront/level-10-evacuation-corridor/concept/rejected/loss-r32-a.png), [.gif](../../campaign/act-2-homefront/level-10-evacuation-corridor/concept/rejected/loss-r32-a.gif); [loss-r32-b.png](../../campaign/act-2-homefront/level-10-evacuation-corridor/concept/loss-r32-b.png), [.gif](../../campaign/act-2-homefront/level-10-evacuation-corridor/concept/loss-r32-b.gif); a in the game: [loss-capture-final-r32-a.png](../../campaign/act-2-homefront/level-10-evacuation-corridor/concept/loss-capture-final-r32-a.png), [.mp4](../../campaign/act-2-homefront/level-10-evacuation-corridor/concept/loss-capture-final-r32-a.mp4) | the review GIFs use a dawn stand-in, not Level 10's section 3; the glow is faint in its first 0.5 s; drawn under the air layer it reads as light on the clouds below rather than a deck above; a's glow sits in the gap between Lifeline One and Three; b's arms are blocky and its column hides the shuttle for 0.2 s || **b** (user), the iris column: written into the game at the close (`python3 tools/art/lance_l10.py`, its `PRODUCTION` now b: `cloud-glow_0..5` and `lance_0..11`, no `lance-flash`, so `LossLooks` draws the column centred on the hit); a moved to `concept/rejected/`; the loss capture (row 14) shows a, taken before the pick |
| 9 | [Wraith decloak sound](../../audio/sfx/README.md#concept-art) | **pick a or b** (`tools/concept/audio/sfx_r32.py`, from Freesound originals), played at the decloak flash's start. **a** "ICEBrk_Break04" by InMotionAudio (CC0) + a synthesized violet shimmer: the shimmer breaking into a glassy ice crack (0.7 s). **b** "Spacey Teleport Rip" by GameAudio (CC0) + "Swirling Velvet Cloak 2" by F.M.Audio (**CC-BY 4.0**): a metallic sci-fi rip over a membrane swish (0.6 s) | [wraith-decloak-r32-a.ogg](../../audio/sfx/concept/rejected/wraith-decloak-r32-a.ogg), [wraith-decloak-r32-b.ogg](../../audio/sfx/concept/wraith-decloak-r32-b.ogg) | **b is CC-BY**: if picked, F.M.Audio goes on the in-game credits screen; not listened to || **b** (user), CC-BY 4.0 (F.M.Audio on the in-game credits roll): produced at the close (`assets/sfx/wraith-decloak-r32-b.ogg`, `tools/art/sfx_originals.py` with `sfx_r32.py`'s `PRODUCTION`), `Sfx.WRAITH_DECLOAK` in place of the placeholder; a moved to `concept/rejected/` |
| 10 | [Mote Swarm sound](../../audio/sfx/README.md#concept-art) | **pick a or b** (`sfx_r32.py`), a one-shot at a swarm's entry and again at its loop-back. **a** "Pigeon flock fly away" by TRP (CC0) + a whoosh: a birdlike rush of small wings, sped up 40 % (1.7 s). **b** "Insect Superfast Wing Flap" by kalhan (CC0) ×5 + a whoosh: an insect chitter (1.7 s) | [mote-swarm-r32-a.ogg](../../audio/sfx/concept/rejected/mote-swarm-r32-a.ogg), [mote-swarm-r32-b.ogg](../../audio/sfx/concept/mote-swarm-r32-b.ogg) | b is **bright** (half its energy above 5 kHz); not listened to || **b** (user): produced at the close (`assets/sfx/mote-swarm-r32-b.ogg`), `Sfx.MOTE_SWARM` at a swarm's entry and each loop-back in place of the placeholder; a moved to `concept/rejected/` |
| 11 | [Lance strike sound](../../audio/sfx/README.md#concept-art) | **pick a or b** (`sfx_r32.py`), the impact 1.2 s into the file, so the game starts it at t 116.8. **a** a synthesized alien falling whine + "Closeup Thunder Strike 01" by loganzsound (CC0): a whine into a thunder crack and a long roll (3.8 s). **b** "Whistling Firework" by magnuswaker (CC0) + "Piercing impact / Stabbing" by Breviceps (CC0) + a sub thump: a whistle into a piercing stab (1.8 s), shorter and drier | [lance-r32-a.ogg](../../audio/sfx/concept/lance-r32-a.ogg), [lance-r32-b.ogg](../../audio/sfx/concept/rejected/lance-r32-b.ogg) | b is about **4 LU louder** (−15.5 against −19.7 LUFS); listen with the music's −6 dB duck in the loss capture (row 14); not listened to || **a** (user): produced at the close (`assets/sfx/lance-r32-a.ogg`), `Sfx.LANCE_STRIKE` in place of the placeholder, still started at t 116.8 (its impact 1.2 s in, checked by `Level10SoundsTest`); b moved to `concept/rejected/` |
| 12 | [Lifeline voice](../../audio/voice/README.md#concept-art) | **audition, pick a or b** (`tools/concept/audio/tts_r32.py`, Chatterbox, radio filter b, neutral): Lifeline One's pilot, also the voice of Two, Four and Five and of the hit line; three lines 1 s apart (t=1, the first hit as Lifeline Two, t=196). **a** Atul Sharma (LibriVox, public domain, clip 116 Hz; 16.7 s); **b** KevinS (LibriVox, public domain, 138 Hz; 17.2 s). Until the pick the speaker is `uncast` and his lines play as text | [voice-lifeline-r32-a.ogg](../../audio/voice/concept/rejected/voice-lifeline-r32-a.ogg), [voice-lifeline-r32-b.ogg](../../audio/voice/concept/voice-lifeline-r32-b.ogg); the clips under [Listening](#listening) | Whisper mishears **"Corridor clear"** in all four readings (check that the word is whole); "Eko" reads "Echo" (the same sound); b's pitch is near the Kilo Lead's (129 Hz) and its hit line jumps an octave on "we're hit!"; measured only, not listened to || **b** (user, 2026-10-08): KevinS cast as Lifeline (`refs/ref-lifeline.wav`); a in `concept/rejected/`, its clip deleted. His seven lines rendered: t=1, t=196 and the hit line once per shuttle. Four hit takes jumped an octave and the t=196 take read 'Core to clear', so five takes are pinned on in-range seeds that read back whole |
| 13 | [Lifeline Three voice](../../audio/voice/README.md#concept-art) | **audition, pick a or b** (`tts_r32.py`): the lost shuttle's pilot, her one t=116 line "Aegis, there's a light above the clouds. What is that—", **cut off** at "that" into 0.3 s of static and a dead channel. **a** Kehinde (LibriVox, public domain, 235 Hz; 4.3 s); **b** Maria Kasper (LibriVox, public domain, 215 Hz; 4.8 s). Both female and higher than the cast's women, so she never sounds like the Lifeline lead | [voice-lifeline-three-r32-a.ogg](../../audio/voice/concept/rejected/voice-lifeline-three-r32-a.ogg), [voice-lifeline-three-r32-b.ogg](../../audio/voice/concept/voice-lifeline-three-r32-b.ogg); the clips under [Listening](#listening) | Whisper hears a's **"Aegis" as "Ages"** (in the raw take too); the cut falls inside "that" (listen whether it reads as cut off or as a glitch); in production `tools/art/voice.py` needs the same cut for a line ending in a dash; measured only, not listened to || **b** (user, 2026-10-08): Maria Kasper cast as Lifeline Three (`refs/ref-lifeline-three.wav`); a in `concept/rejected/`, its clip deleted. `tools/art/voice.py` now cuts off any radio line ending in a dash, as in the audition; her t=116 line is rendered (4.6 s), cut after 'that' into 0.3 s of static and a dead channel |
| 14 | [Level 10 in the game](../../campaign/act-2-homefront/level-10-evacuation-corridor/README.md#concept-art) | game captures, **taken again after the fix pass, from the fixed build** (same file names; the run's options and numbers are in the level's [prompts.md](../../campaign/act-2-homefront/level-10-evacuation-corridor/concept/prompts.md)). **Level 10** at medium with the plan's fit and Rook's Autocannon, `--invulnerable`, flown by a key script: the liftoff off the pads, a cloaked Wraith over the band, its decloak and its fan straight up through the band, Rook in Trail, the tracker's bars, a lost shuttle's glide, the lance on Lifeline Three, a Mote Swarm's loop-back, the ferry hatch and its crate, the climb-out under the Wraith finale, the debrief; 12 panels and the whole run with sound. **The loss capture**: look a in the game, t 110–122 with the lance's sound and the music's duck. **The readability strip**: the Wraith cloaked over the city and over the lagoon, its decloak and hold, motes, a shuttle's banks and damage smoke, the hatch shut and open, at 1:1 and 2× | [level-10-capture-final-r32-a.png](../../campaign/act-2-homefront/level-10-evacuation-corridor/concept/level-10-capture-final-r32-a.png), [.mp4](../../campaign/act-2-homefront/level-10-evacuation-corridor/concept/level-10-capture-final-r32-a.mp4), [loss-capture-final-r32-a.png](../../campaign/act-2-homefront/level-10-evacuation-corridor/concept/loss-capture-final-r32-a.png), [.mp4](../../campaign/act-2-homefront/level-10-evacuation-corridor/concept/loss-capture-final-r32-a.mp4), [level-10-readability-r32-a.png](../../campaign/act-2-homefront/level-10-evacuation-corridor/concept/level-10-readability-r32-a.png) | **listen** for the −6 dB duck at the loss and the megacity → ocean ambience crossfade at section 4 (t 130): neither was checked by ear; the sounds are the placeholders and the loss look a; the run shown brought 3 of 4 home (a near-identical run brought 1: results vary); the wreck's last second is a small dark shape against the city; the Lifeline lines show as text; the cloak is faint over the city; `--invulnerable` keeps the fail from showing || **accepted** (user): the captures as taken (with the placeholder sounds and loss look a; the picks are in the game since) |
| 15 | [Level 10's lines voiced](../../audio/voice/README.md#decisions) | **listening row**: 22 new lines rendered with the cast voices (`tools/art/voice.py`): the two briefing pages (dry), the radio lines with the `first-decloak`, `first-loop-back`, `scripted-loss` and five `ally-lost` lines, the four level-end lines by shuttles home, the mission failed line and the ferry's line (Okafor 13, Varga 5, Rook 4). Quoted under [Listening](#listening). Accept as rendered, or name the lines to re-render | the files under [Listening](#listening) | listen to **Okafor's pause** of about 0.6–1 s between "Lifeline" and its number (every `ally-lost` take and the `scripted-loss` line do it); **Varga's page 2** for "Vrell" (pinned; it reads back only with a name prompt, "rail" without: an imperfect pronunciation) and "Flocks" (once "Flock sweep"); the **four-home end line** (pinned; the key's seed read "eight to hundred and eighty"); Okafor's page 1 (pinned, "Fides shuttles") and the ferry line (pinned, "berry's"); "We lost Lifeline Three" is rendered by the `{ally}` expansion but never plays (Lifeline Three is lost only by the script); reviewed by Whisper only || **accepted as rendered** (user): all 22 lines, the pinned takes included; none to re-render |
| 16 | [Texts for review](#texts-for-review) | **text review** (Level 10's document is `design: review`): the two briefing pages, Rook's hangar teaser, Varga's four intel lines, the radio script with the new events, the four level-end lines, the mission failed line, the ferry's line and the names on screen (the briefing's objective `ESCORT THE 5 SHUTTLES TO THE END`, the tracker `SHUTTLES n / 4`, the warning `NO REAR WEAPON FITTED`, the debrief row `SHUTTLES HOME n / 4`), all quoted under [Texts for review](#texts-for-review). Approve, or say what to change | — | changing a voiced text means a re-render (row 15); the briefing counts **5** shuttles while the tracker and the debrief count the **4** saveable ones (a question: keep it so?); Varga's L1 line says "a third" from behind, the profile's count by `from` 16 % (by threat about a third, confirmation 21k) || **approved** (user): every text as quoted, the briefing's 5 shuttles against the tracker's 4 kept; Level 10's document leaves `review` for `approved` |
| 17 | [Part D numbers](#part-d-numbers) | accept or change the numbers listed under [Part D numbers](#part-d-numbers): density, pacing, the credit budget and `bounty_scale`, the Wraith's and the Mote Swarm's numbers, the rear check, the shuttles' armour and the autopilot's results, the hatch | — | **hard keeps 1 of 4** on every seed (the user's decision, recorded); the density is **97.8 a minute** against the minimum 50 (the pacing rule needs it, as Level 09's 84); the perfect run is 1.64 × budget || **accepted** (user), hard keeping 1 of 4 included |
| 18 | [Part D decisions](#user-decisions-of-part-d-for-the-record) | **for the record, not to choose again**: the user decisions D1–D12, the stated defaults and the three decisions taken during the build (the Wraith's fan straight up, the capture's bugs fixed, hard keeps 1 of 4), listed under [User decisions of part D](#user-decisions-of-part-d-for-the-record); check they were carried out | — | D10 and D12 are carried out as far as this round goes: the shuttle, the loss look, the three sounds and the two voices wait for rows 7–13 || **checked** (user): every decision carried out (D10 and D12 with rows 7–13) |
| 19 | [Build choices: Wraith and Mote Swarm](#build-choices-wraith-and-mote-swarm) | **yes or no, each**: (a) the bursts 0.5 s and 1.7 s into the hold, two only; (b) the exit lane; (c) the hold depth 470–520 px; (d) the `rear ambush` of 1–4 in even lanes; (e) the flock tuning; (f) the leader point a route, not a unit; (g) motes removed 6 s after their route ends; (h) `first-loop-back` at the re-entry; (i) the `Trig` table. Details under [Build choices](#build-choices-wraith-and-mote-swarm) | — | (a) leaves hard's 3.0 s hold without a third burst || **confirmed** (user): (a)–(i) as built |
| 20 | [Build choices: the shuttles](#build-choices-the-shuttles) | **yes or no, each**: (a) the stations, sways, pads and x range; (b) untouchable on the pads, all five lifting off together; (c) a contact hurts once per overlap, a Wraith's body 15; (d) a bullet passes an untouchable shuttle and hurts the next; (e) a rammer dying on a shuttle is paid; (f) full bank at 20 px/s; (g) the wreck sliding 64 px outward, its glide; (h) Okafor's `ally-lost` at every loss, the first included; (i) `--invulnerable` keeps an air escort from failing. Details under [Build choices](#build-choices-the-shuttles) | — | (f) still shows only about ±1 bank frame (row 7) || **confirmed** (user): (a)–(i) as built |
| 21 | [Build choices: level, art, UI and radio](#build-choices-level-art-ui-and-radio) | **yes or no, each**: (a) sections 1–3 night-navy; (b) gunships without a rotor; (c) the hatch crate's legibility; (d) decloak frame 1 blobby; (e) the secret counts when the hatch opens; (f) the banner above the band; (g) the home bar in pale mint; (h) the popcorn routes round the band; (i) the Side Splitter does not silence the rear warning; (j) the Wraith's drops moved to Needlers, the counts authored; (k) "a third" by threat; (l) the glow under the air layer; (m) the tracker's bars; (n) the intel's long level name; (o) a near-dead shuttle's empty-looking bar in the climb-out; (p) the 12 s ferry crate. Details under [Build choices](#build-choices-level-art-ui-and-radio) | — | (a)–(d) are weak spots kept as built unless the user asks for a redo || **confirmed** (user): (a)–(p) as built; (a) the night-navy sections 1–3 and (o) a near-dead shuttle's bar in the climb-out kept |

## Part D numbers and texts

The numbers and texts the agents chose or measured to build part D (rows 16–21). The details are in
the parts' READMEs (Decisions of 2026-10-08). The design source is the gap analysis of part D (D1–D12
of 2026-10-08).

### Texts for review

From [Level 10's data](../../campaign/act-2-homefront/level-10-evacuation-corridor/data.yaml).

**Level 10 briefing**, two pages, one screen each
([Level 10](../../campaign/act-2-homefront/level-10-evacuation-corridor/README.md#briefing)):

> **Commander Okafor** (`level-10-evacuation-route`): "Nova Lagos evacuates at first light, Lancer.
> Five shuttles lift from Eko spaceport with eleven hundred people aboard. You and Rook are their
> corridor to the lagoon. Every shuttle we lose is two hundred and twenty lives."
>
> **Dr. Varga** (`level-10-wraith-scan`): "The Vrell hunt evacuation flights from behind. Cloaked
> flyers pass overhead, then decloak at your six and fire up the screen. Flocks sweep past and turn
> back. Fit a rear gun, Lancer."

**Hangar teaser** (Rook, `LT. K. TANAKA / AEGIS TWO`, not voiced): "Shuttles to babysit and bugs
that hit from behind. Bolt a gun on your tail, Lancer."

**Varga's intel lines** (one per sensor-suite level, `IntelPanelLayoutTest`):

| Sensor suite | Line |
|---|---|
| none | "Nova Lagos's outskirts at first light, Lancer. Flyers only, and five shuttles to keep alive." |
| L1 | "A third of it comes from behind you, Lancer. Five shuttles in one corridor: keep them flying." |
| L2 | "New: the Wraith, cloaked until it strikes from behind, and the Mote Swarm, a flock that turns back." |
| L3 | "A rear gun is a must: only homing finds a cloaked Wraith. One cache: a capsized ferry's hatch. Shoot it open." |

**Radio script** ([Level 10](../../campaign/act-2-homefront/level-10-evacuation-corridor/README.md#radio-chatter);
L10 has no holds, so script time is real time):

| Trigger | Speaker | Line |
|---|---|---|
| t=1 | Lifeline One (text until cast, row 12) | "Eko Control, Lifeline One. Five birds, eleven hundred souls. We're going." |
| t=12 | Okafor | "Lifeline flight, Aegis has you. Stay in the corridor and stay together." |
| t=24 | Rook (`requires: escort`) | "Something's coming down the left. A lot of somethings." |
| t=32 | Varga | "That's a flock, not a formation. They steer like starlings." |
| First decloak (`first-decloak`) | Varga | "It hid in plain sight! Cloaked, only homing touches it. Rear guns!" |
| First loop-back (`first-loop-back`) | Rook (`requires: escort`) | "The flock's turned around. They're coming up behind us!" |
| First shuttle hit (`first-ally-hit`) | Lifeline (text until cast) | "Lifeline {ally}, we're hit! Still flying. Please stay close!" |
| t=116 | Lifeline Three (text until cast, row 13) | "Aegis, there's a light above the clouds. What is that—" |
| Scripted loss (`scripted-loss`) | Okafor | "Lifeline Three is down. Keep the others moving, Lancer." |
| t=131.5 | Rook (`requires: escort`) | "Where did that come from? Nothing on my scope!" |
| t=139 | Varga | "That shot came from above the cloud deck. Something big is sitting up there, out of reach." |
| A shuttle is lost (`ally-lost`) | Okafor | "We lost Lifeline {ally}. Stay on the others." |
| Ferry cache (secret) | Varga | "That ferry's hold had a CDF supply crate in it. Take it, Lancer." |
| t=180 | The Choir (distorted) | "[the Choir sings]" (its round-29 stage sound) |
| t=188.5 | Rook (`requires: escort`) | "Last one out of Lagos, get the lights." |
| t=196 | Lifeline One (text until cast) | "Corridor clear. We can see the sky. Thank you, Aegis. Thank you." |
| Level end, 4 home | Okafor | "Four of five made orbit. Eight hundred and eighty people will see tomorrow. Remember the ones you got home." |
| Level end, 3 home | Okafor | "Three shuttles made orbit. Six hundred and sixty people will see tomorrow. Good work, Lancer." |
| Level end, 2 home | Okafor | "Two shuttles made orbit. Four hundred and forty people will see tomorrow. It's not nothing, Lancer." |
| Level end, 1 home | Okafor | "One shuttle made orbit. Two hundred and twenty people will see tomorrow. Hold on to that." |
| The four saveable shuttles lost (the mission failed screen) | Okafor (grim) | "We've lost the corridor, Lancer. Pull back." |

**Names on screen**: the briefing's objectives line **`ESCORT THE 5 SHUTTLES TO THE END`** (five in
all; four of them saveable, Lifeline Three lost by the script); the objective field from sensor L1
`ESCORT 5 SHUTTLES`; the tracker **`SHUTTLES n / 4`** over five armour bars; the launch warning
**`NO REAR WEAPON FITTED`**; the debrief's escort row **`SHUTTLES HOME n / 4`** with its pay
(`+ 40 CR` a shuttle); no secondary objective, so no secondary row or `BONUS` line.

### Part D numbers

From [Level 10](../../campaign/act-2-homefront/level-10-evacuation-corridor/README.md) and its data:

- **Length:** 190 px/s, ≈ 38,000 px = **200 s**, launch 5 s; no holds, so script time is real
  time. The liftoff t 1–7, the scripted loss at t=118, the climb-out t 196–198.
- **Density:** **318 enemies at medium** (easy **249**, hard **387**), **97.8 a minute** over the
  195 s after the launch (Act 2's minimum is 50): Skitter 169, Needler 47, Mote Swarm 80 (in
  motes; swarms of 10, 6, 14, 16, 8, 8 + 8 and 10), Wraith 11 (easy 9, hard 13: the t=170 pair),
  Stinger 11. By `from`: 262 front, 52 rear, 4 sides, so **rear 16 %** by the intel's count; the 48
  motes of the four looping swarms attack from the rear too (about a third by threat).
- **Pacing** (`PacingTest`, the plan's fit with Rook): no pause over 3 s on medium and hard; on easy
  only the quiet end (from t≈195).
- **Credit budget:** budget(10) = 700 × 1.07⁹ ≈ **1,287**. **`bounty_scale` 0.56**: the typical
  haul is **1,316** (+2.2 %), a perfect run **2,113** (1.64 × budget). Paid at Level 10 (× 1.6 ×
  0.56, rounded per kill): Skitter 4, Mote 2, Needler 11, Stinger 13, Wraith 27. The data keeps Act
  1 terms: the ferry crate 100 (pays 160), the escort 25 per saveable shuttle home (pays 40; up to
  160; Lifeline Three is not paid). The gap check's estimate was ≈ 0.85–0.9 for about 210 enemies.
- **The Wraith** ([stat block](../../enemies/air/wraith/README.md#stat-block)): **HP 16** (easy 12,
  hard 21; D7 = a, was 27); 72×72, hit box 50×40, `medium`; cloaked on `high-air` at 200 px/s, the
  0.4 s flash, a 2.5 s hold (hard 3.0 s), two 5-shot bursts (hard 7) 0.12 s apart at 220 px/s,
  `medium` = 6, straight up as a fixed **40° fan**; out at 120 px/s; bounty 30 (pays 27).
- **The rear check** (`BalanceTest`, D7 = a): the plan's L10 rear DPS **10** (the Tail Gun L1) →
  16 ÷ 10 = **1.6 s of the 2.9 s** flash and hold (0.55, the rule ≤ 0.6). At the L10 reference DPS
  66 it dies in 0.24 s, an accepted exception like the Ravager's. The plan's L10 visit buys the
  Tail Gun and **refits Rook's Autocannon** (the new `fit` action).
- **The Mote Swarm** ([stat block](../../enemies/air/mote-swarm/README.md#stat-block)): 1 HP, 16×16,
  hit box 10×10, `tiny`; flock cruise 200 px/s, 260 px/s diving after a loop-back; contact 6,
  destroyed by the impact; swarms of 6–24 (the formation lever ±20 %); one loop-back, hard two;
  bounty 2.
- **The shuttles** ([allies](../../allies/README.md#evacuation-shuttle)): armour **120** (easy
  **180**, hard **90**), no shield; hit box 48×28; `small` bullet 4, `medium` 6, a mote's contact 6,
  a Wraith's body 15; stations in the band 140–300 px below the top edge, x 120–360; unchanged by
  the measurements.
- **Autopilot** (`Level10Test`, the plan's fit with Rook, which does not guard the shuttles): easy
  **4 of 4** home on all 9 seeds (A); medium **3 of 4** on all 17 seeds (A, both Airstrike charges,
  the crate caught); hard **1 of 4** on all 9 seeds (B; the user keeps it: hard is meant to be
  hard). Hard's 90 armour raised to 120 brought only 1–2 home, so it stays.
- **The ferry hatch**: **50 HP** (the default's 8 opened at the top edge under passing fire):
  about a second of the forward guns lined up on it. With the fire held before the hatch enters, the
  plan's fit and Rook open it **142 px** down (every gun at its top level: 79 px); the crate drifts
  40 px/s and lives **12 s** in this level (`crate_seconds`, a new level key; every other pickup and
  level keeps 6 s), so it reaches a ship on its start line from anywhere the hatch can open (the
  worst case, the top edge, takes 10.6 s). The re-take's capture first showed a 6 s crate expiring
  short of the start line. Opened and caught on easy and medium on every seed; on hard the
  autopilot opens it on 3 of 9 and catches each one it opens.
- **Radio** (`RadioTimelineTest`, the 1 s rule, with and without Rook): no voiced timed line starts
  more than 1 s late; the draft's five clashes are retimed (see the radio script).

### User decisions of part D (for the record)

Not to choose again; check they were carried out (row 18).

- [x] **D1 = a**: the shuttles hold authored stations in the band y = 140–300 and drift on a
  deterministic lane sway; they never react to threats.
- [x] **D2 = a**: every enemy bullet (spent) and every `air` contact hurts a shuttle, small rammers
  die on it; tuned until the autopilot keeps at least 3 of the 4 saveable on medium (it does on all
  17 seeds).
- [x] **D3 = a**: the two-line tracker, `SHUTTLES n / 4` over five armour bars.
- [x] **D4 = a**: Lifeline Three untouchable before t=118, fire and contact pass through it, the
  glow from t=116, the lance; it costs nothing (no pay, no fail, no loss cue); `scripted-loss`.
- [x] **D5 = a**: the mission fails at once when the four saveable shuttles are lost; 25 per
  shuttle home (Act 1 terms); no secondary; four level-end lines.
- [x] **D6 = a**: the Wraith as its stat block (cloaked from the top, rear entry, decloak, hold,
  decloaked exit up a side lane).
- [x] **D7 = a**: Wraith HP 16, `BalanceTest`'s rear check, the plan refitting Rook's Autocannon.
- [x] **D8 = a**: real flocking (boids round a leader route).
- [x] **D9 = a**: `required: [rear]`, the launch warning at every sensor level.
- [x] **D10 = a**: straight to production (rows 1–6); a/b only for the shuttle and the loss's look
  (rows 7–8), whose production follows this round.
- [x] **D11 = a**: first light.
- [x] **D12 = a**: two auditions, Lifeline and Lifeline Three (rows 12–13), `uncast` and their
  lines as text until then.
- [x] **Defaults** (all applied): the data in Act 1 terms with a `bounty_scale`, the scripted
  shuttle outside the haul; Act 2 popcorn to ≥ 50 a minute and the 3 s rule, the first enemies at
  t≈8; 190 px/s; 3 s edge warnings, the swarm's 1.5 s gap; directions by `from`, the looping swarms
  `from: front` with the share by threat in the text; the formations `swarm` and `rear ambush`; the
  veins drawn only; hard's 7-shot bursts, 3.0 s holds, the t=170 pair, two loop-backs; easy motes
  as medium; the radio retimed, Rook's t=50 line dropped, Varga's Wraith line on `first-decloak`
  without "and beam", Rook's flock line on `first-loop-back`, Okafor's loss line on
  `scripted-loss` without the pause, `ally-lost` for every player loss, the hit line Lifeline's
  with `{ally}`, a mission failed line, "two hundred and twenty lives", Rook's lines `requires:
  escort`; two one-screen briefing pages with images, Rook's teaser, Varga's four lines; `traits:
  [rear, spread]`; the shuttles untouchable in the liftoff and the climb-out, the glide into `far`,
  armour 180 / 120 / 90; the ferry hatch with a hidden crate (its HP raised by the fix pass); the
  overdrive and armour patch carried (moved to Needlers, confirmation 21j); "Homefront"'s base stem
  from section 2 and full mix from section 3, the −6 dB duck, the ambience crossfade at section 4;
  no change to Rook's AI.
- [x] **The Wraith's bursts straight up** (user, 2026-10-08): a fixed 40° fan up the screen through
  the shuttle band, aimed at nobody, exempt from the 72 px no-fire distance.
- [x] **All the capture's bugs fixed** (user, 2026-10-08): the popcorn routes round the band, the
  wreck's glide, the ferry hatch and its crate, the banner above the band, the home bar.
- [x] **Hard keeps 1 of 4** (user, 2026-10-08): hard is meant to be hard; its numbers stay as built.

### Build choices: Wraith and Mote Swarm

Each **yes** (keep as built) or **no** (say what to change), row 19
([wraith](../../enemies/air/wraith/README.md#behaviour),
[mote swarm](../../enemies/air/mote-swarm/README.md#behaviour),
[schemas](../../tech/architecture/README.md#data-file-schemas)):

- **(a) The bursts 0.5 s and 1.7 s into the hold** (0.9 s after it stops, then 1.2 s later): two
  volleys only, so hard's 3.0 s hold fires no third.
- **(b) The exit lane**: after the hold it slides to the nearer side lane (its centre 40 px from
  that edge, a tie to the left) while it climbs 60 px, then straight up from 120 px above its hold
  point at 120 px/s, clear of the shuttle band (x 120–360).
- **(c) The hold depth** seeded per unit between 470 and 520 px below the top edge (20–70 px above
  the bottom edge).
- **(d) The `rear ambush` of 1–4** Wraiths, their lanes spread evenly across the bottom edge
  (x = (i + 1) · 480 ÷ (n + 1)); a lone one down the centre as its introduction.
- **(e) The flock tuning**: nearest neighbours about 14 px apart, the cloud within about 60 px of
  the leader point; members fly 0.8–1.3 × the cruise as they are near or far, turn at most
  `turn_rate`, and pairs closer than the 18 px separation move apart by 0.3 of the shortfall a
  step; the leader point loops back at the dive speed 260 px/s.
- **(f) The leader point is a route, not a unit**: killing motes never stops it.
- **(g) Motes removed 6 s after their route ends**: off the field at the route's end they are gone
  at once (escaped), those still on it 6 s later too.
- **(h) `first-loop-back` at the re-entry**: the event fires when the leader point re-enters below
  the bottom edge as its warning ends; as a scripted Rook line it withdraws his rear bark of that
  loop-back.
- **(i) The `Trig` table**: the simulation's sine and cosine come from a table filled once by
  `StrictMath` (deterministic and allocation-free; `StrictMath.sin` allocates on JDK 21).

### Build choices: the shuttles

Each **yes** or **no**, row 20 ([Level 10](../../campaign/act-2-homefront/level-10-evacuation-corridor/README.md#the-shuttles),
[allies](../../allies/README.md#decisions)):

- **(a) The stations, sways and pads' first values** (the table in Level 10's *The shuttles*), the
  x range 120–360 that keeps the Wraiths' side lanes clear.
- **(b) Untouchable on the pads**, scrolling with the ground before the liftoff, and **all five
  lifting off together**, t 1–7.
- **(c) A contact hurts a shuttle once per overlap** (a new contact after the enemy left it again);
  a Wraith's body hurts it once for 15 (the `medium` contact).
- **(d) A bullet hits the first shuttle it touches that can be hit**: one passing through an
  untouchable shuttle hurts the next.
- **(e) A rammer that dies on a shuttle is paid** as when it rams the ship (a mote destroyed by
  contact with a shuttle included).
- **(f) Full bank at 20 px/s sideways** (five frames; the first draft's 60 never reached a bank
  frame at the lanes' sway).
- **(g) The wreck slides 64 px outward**, away from the band's middle, so the shuttle below does not
  hide it; it plays the wreck frames down to far's scale (0.47) over the whole 3 s, darkens from the
  hit, fades only in its last 0.36 s and trails a plume of large dark smoke.
- **(h) Okafor's `ally-lost` line at every player-caused loss, the first included** (the
  `first-ally-lost` cue has no line of its own here).
- **(i) `--invulnerable` keeps an air escort from failing** (a debug option, for captures).

### Build choices: level, art, UI and radio

Each **yes** or **no**, row 21:

- **(a) Sections 1–3 read night-navy** more than dawn (the backdrop, row 4): keep, or lift them
  further toward first light.
- **(b) The CDF gunships are drawn without a rotor** (Level 08's gunship, as the draft's
  "helicopters").
- **(c) The hatch's crate is barely legible** in the open hold (row 5).
- **(d) The decloak flash's frame 1 looks blobby** (row 1).
- **(e) The secret counts when the hatch opens** (as in Levels 03–09: the grade and the Explorer
  bonus); the crate's credits only when it is caught.
- **(f) The wave banner above the band with an air escort**: 97–127 px below the top edge, clear of
  Lifeline One's station ([HUD](../../ui/hud/README.md#decisions)).
- **(g) A shuttle home after the climb-out**: its bar full in **pale mint** (`C8FFE0`) under a green
  glow, unlike a flying (green, white, amber) or a lost (dark) one's.
- **(h) The popcorn routes round the band**: a Skitter snake comes down the side lane of its edge
  (x 50 or 430) to 250 px, curls across below the band (340–400 px) and leaves at the bottom of the
  other side; a stream from the left comes down x = 70 to 300 px and runs diagonally to the bottom
  right (from the right mirrored); at least 47 px clear of the band's hit boxes.
- **(i) The Side Splitter in the rear slot does not silence the rear warning**: only the Tail Gun,
  the Fan Blaster and the Proximity Mines carry `rear`; Rook's guns and homing never do
  ([hangar](../../ui/hangar/README.md#decisions)).
- **(j) The Wraith carries nothing**: the overdrive and the armour patch drop from the last Needler
  of the t=111.5 and t=154.5 V-wings (a Wraith dies 20–70 px above the bottom edge, where a drop
  drifts off before it can be caught); the Wraith counts are authored (the finale three on every
  difficulty, easy one fewer at t=106 and t=157).
- **(k) "A third" by threat**: Varga's L1 line and the profile's text count the looping motes as
  rear threat; the profile's direction count by `from` says 16 %.
- **(l) The loss's glow is drawn under the air layer**, not on `deep` (which the section's ground
  hides), on no layer anything can hit.
- **(m) The tracker's bars**: 32×8 px, 41 px apart, filled by the armour share (full while lifting
  off and while Lifeline Three is untouchable); a lost bar flashes red for 1.5 s then stays dark,
  Lifeline Three's goes dark at once; on a fail line one flashes red as Level 04's.
- **(n) The intel's long level name**: `EVACUATION CORRIDOR` takes the label font where the body
  font would run into the sensor chip.
- **(o) A nearly dead shuttle's bar during the climb-out**: in the capture, Lifeline One's and
  Four's bars held 4–5 px of amber and read as empty for about 2 s (t 195.5–197.5) before they
  turned mint at home; briefly it looks like a loss. Keep, or give a climbing shuttle a minimum bar.
- **(p) The ferry crate lives 12 s in Level 10** (`crate_seconds`), twice the usual 6 s, so it
  reaches the start line from wherever the hatch opens.

## Listening

The 22 new voiced lines (row 15) as the game plays them, from `assets/voice/`, matched to their
lines by the voice key in `tools/art/voice.py`'s render log; then the Choir's reused stage sound
and the four audition reference clips (rows 12–13, in `design/audio/voice/refs/`, not a `concept/`
directory) with their transcripts. The auditions themselves are on the board's cards.

| Line | Speaker | Text | Take |
|---|---|---|---|
| Briefing page 1 | Okafor | "Nova Lagos evacuates at first light, Lancer…" (pinned take; listen for "Five shuttles", 18.4 s) | [play](../../../assets/voice/okafor/57cca67e43d2.ogg) |
| Briefing page 2 | Varga | "The Vrell hunt evacuation flights from behind…" (pinned take; listen for "Vrell" and "Flocks", 12.2 s) | [play](../../../assets/voice/varga/a86c85a4cc53.ogg) |
| t=12 | Okafor | "Lifeline flight, Aegis has you. Stay in the corridor and stay together." | [play](../../../assets/voice/okafor/6865af49d2d7.ogg) |
| t=24 | Rook | "Something's coming down the left. A lot of somethings." | [play](../../../assets/voice/rook/10fcf90b6794.ogg) |
| t=32 | Varga | "That's a flock, not a formation. They steer like starlings." | [play](../../../assets/voice/varga/04b12297fa5d.ogg) |
| First decloak | Varga | "It hid in plain sight! Cloaked, only homing touches it. Rear guns!" | [play](../../../assets/voice/varga/5a381624e8ba.ogg) |
| First loop-back | Rook | "The flock's turned around. They're coming up behind us!" | [play](../../../assets/voice/rook/13e9084ba1c8.ogg) |
| Scripted loss | Okafor | "Lifeline Three is down. Keep the others moving, Lancer." (the pause after "Lifeline") | [play](../../../assets/voice/okafor/41955ba3506f.ogg) |
| t=131.5 | Rook | "Where did that come from? Nothing on my scope!" | [play](../../../assets/voice/rook/ebc17cf3f4c4.ogg) |
| t=139 | Varga | "That shot came from above the cloud deck. Something big is sitting up there, out of reach." | [play](../../../assets/voice/varga/a99785d48c3a.ogg) |
| Ally lost, One | Okafor | "We lost Lifeline One. Stay on the others." (the pause after "Lifeline") | [play](../../../assets/voice/okafor/43c0cab7f15c.ogg) |
| Ally lost, Two | Okafor | "We lost Lifeline Two…" | [play](../../../assets/voice/okafor/5f76f1f7bcc8.ogg) |
| Ally lost, Three | Okafor | "We lost Lifeline Three…" (rendered by the expansion, never played) | [play](../../../assets/voice/okafor/31447ac218a4.ogg) |
| Ally lost, Four | Okafor | "We lost Lifeline Four…" | [play](../../../assets/voice/okafor/ce5b2681f300.ogg) |
| Ally lost, Five | Okafor | "We lost Lifeline Five…" | [play](../../../assets/voice/okafor/a1971dbc7912.ogg) |
| Ferry cache | Varga | "That ferry's hold had a CDF supply crate in it. Take it, Lancer." (pinned take; listen for "ferry's") | [play](../../../assets/voice/varga/ec548c3af77b.ogg) |
| t=188.5 | Rook | "Last one out of Lagos, get the lights." | [play](../../../assets/voice/rook/da3c9b720111.ogg) |
| Level end, 4 home | Okafor | "Four of five made orbit. Eight hundred and eighty people will see tomorrow. Remember the ones you got home." (pinned take; listen for "eight hundred and eighty") | [play](../../../assets/voice/okafor/9043598844d3.ogg) |
| Level end, 3 home | Okafor | "Three shuttles made orbit. Six hundred and sixty people…" | [play](../../../assets/voice/okafor/5aec6bafbbdf.ogg) |
| Level end, 2 home | Okafor | "Two shuttles made orbit. Four hundred and forty people…" | [play](../../../assets/voice/okafor/02973b9c7adb.ogg) |
| Level end, 1 home | Okafor | "One shuttle made orbit. Two hundred and twenty people…" | [play](../../../assets/voice/okafor/ba1c916ec73b.ogg) |
| Mission failed | Okafor (grim) | "We've lost the corridor, Lancer. Pull back." | [play](../../../assets/voice/okafor/dc3afd2a7d34.ogg) |
| t=180 | The Choir | "[the Choir sings]" (round 29's stage sound, reused) | [play](../../../assets/voice/choir/voice-choir-sings-r29-b.ogg) |
| Audition clip, Lifeline a | Atul Sharma | the reader's LibriVox clip, 116 Hz (not picked: deleted at the close with its CREDITS.md row) | — |
| Audition clip, Lifeline b | KevinS | the reader's LibriVox clip, 138 Hz, picked: renamed at the close ([transcript](../../audio/voice/refs/ref-lifeline.txt)) | [play](../../audio/voice/refs/ref-lifeline.wav) |
| Audition clip, Lifeline Three a | Kehinde | the reader's LibriVox clip, 235 Hz (not picked: deleted at the close with its CREDITS.md row) | — |
| Audition clip, Lifeline Three b | Maria Kasper | the reader's LibriVox clip, 215 Hz, picked: renamed at the close ([transcript](../../audio/voice/refs/ref-lifeline-three.txt)) | [play](../../audio/voice/refs/ref-lifeline-three.wav) |

## Notes

- Review file names: `<subject>-final-r32-a.png/.gif` (production art), `level-10-capture-final-r32-a`,
  `loss-capture-final-r32-a` and `level-10-readability-r32-a` (game captures), and
  `<subject>-r32-a/b` (the seven a/b picks), in each part's `concept/`. Their `prompts.md` entries
  say how they are made. Nothing in `assets/` is hand-edited.
- The board shows the two videos (`level-10-capture-final-r32-a.mp4`, `loss-capture-final-r32-a.mp4`)
  as links ("open"). The audition reference clips live in `design/audio/voice/refs/`, not in a
  `concept/` directory, so the board shows them in the *Listening* table, not on cards.
- The captures were taken again after the fix pass with the same file names (the run shown: 207 of
  318, 3 of 4 home, the crate caught, grade A, 2,057 credits); the crate's 12 s life came after the
  re-take, where the ship flew up to meet it.
- After the choices (at the close):
  - the approved parts get `art: final`;
  - shuttle b: `python3 tools/art/shuttle.py --variant b` writes it under the game's names (and the
    route briefing image is redrawn); loss look b: `python3 tools/art/lance_l10.py --variant b` (no
    `lance-flash` in b); the rejected files move to `concept/rejected/`;
  - the picked sounds go to `assets/sfx/` (a `PRODUCTION` entry in `sfx_r32.py`) and replace the
    placeholders in `Sfx` (the lance's impact 1.2 s into its file, started at t 116.8); a CC-BY
    pick (decloak b) gets its credits-screen entry; the rejected files move to `concept/rejected/`;
  - the cast Lifeline voices render their lines (`uncast` dropped, `AUDITIONING` emptied; Lifeline
    Three's cut needs `tools/art/voice.py` to cut a line ending in a dash), the rejected clips are
    deleted with their CREDITS.md rows;
  - Level 10's design leaves `review` once the texts are approved.

## Decisions

- 2026-10-08: Opened with M5 part D.
- 2026-10-08: Closed (user verdict on every row): the production art approved as final (rows 1–6);
  the evacuation shuttle **a** (b rejected); the scripted loss's look **b** "Iris column" (a's
  thorn spear rejected), written into the game at the close; the decloak sound **b** (CC-BY 4.0,
  F.M.Audio on the credits roll; a, an ice crack over a shimmer, rejected), the Mote Swarm **b** (a,
  a pigeon flock, rejected) and the lance **a** (b, a whistle into a stab, rejected), produced into
  `assets/sfx/` and played in place of the placeholders; the Lifeline voice **b** (KevinS) and the
  Lifeline Three voice **b** (Maria Kasper), cast and their eight lines rendered (five takes pinned); the captures, the 22 voiced
  lines, the texts (Level 10 now `approved`), the part D numbers, the decisions and the build
  choices 19(a)–(i), 20(a)–(i) and 21(a)–(p) accepted as built, 21(a) and 21(o) kept as they are.
  The rejected files are in each part's `concept/rejected/`.
