---
title: Concept round 26 — M4 part H, the Act 1 close-out
design: review
implementation: n/a
art: proposed
depends-on: [../../campaign/act-1-first-contact, ../../ui/hud, ../../ui/hangar, ../../ui/credits, ../../audio/music, ../../audio/sfx, ../../audio/voice]
updated: 2026-10-05
---

# Concept round 26 — M4 part H, the Act 1 close-out

## Summary

The production-art round of M4 part H ([roadmap](../../tech/roadmap/README.md#m4-parts)), the
close-out of Act 1: the Skitter's, Needler's and Scuttler's own deaths, the Smart Bomb's burst and
ring, the boss bar's plate, the still behind the Act 1 title card, the ship's engine flames, damage
smoke and sparks, the shield's hex ring and the `medium` enemy bullet, rendered as **final** art by
the generators in [tools/art/](../../../tools/art/README.md) into `assets/`; game captures of the
runtime drop shadows, the HUD warnings, the hangar's test fire and the credits roll; the rest of the
music as finals (D6 = A, as they are); the sounds the SFX pass wired; Okafor's low-armour line;
Varga's 28 intel lines; the numbers the agents chose to close part H; and a listen-through of the
128 voiced lines of the act briefing and Levels 01–06 that no round has reviewed yet. Open
[index.html](index.html) in a browser (regenerate with `python3 tools/concept/board.py 26`). Every
art choice is *approve as final* or *redo* (say what to change); a row with a **question** needs an
answer too; only an approved part gets `art: final`, and with every art row approved Act 1's art
becomes final. In the game: `--level 4 --special smart-bomb:3` (Scuttler deaths, the Smart Bomb),
`--level 5` (the mid-boss bar), `--level 7` (the act-boss bar), Esc → *Abort to hangar* for the
test fire, the main menu's CREDITS for the roll.

## Choices

| # | Part | What is proposed | Review files | Weak spots (Claude) | Outcome |
|---|---|---|---|---|---|
| 1 | [Skitter](../../enemies/air/skitter/README.md), [Needler](../../enemies/air/needler/README.md) and [Scuttler](../../enemies/ground/scuttler/README.md) deaths | production art (`tools/art/vrell_deaths.py`), each unit's own death layered on its explosion-ladder burst and picked up by name (`-tatters` pieces, `-death` glow): the **Skitter** (32×32: the wings torn off, the body cracked into two chitin flakes with the tail spikes, a teal glow flash after the tiny pop), the **Needler** (48×48: ivory shards, five carapace plates, the claws and thorn launcher, a violet flash and sparks with the small burst), the **Scuttler** (96×96, 16 frames: legs, claws and shell fragments settling on the ground, a lime flash, ichor spray and olive mist with the medium burst; the husk stays) | `skitter-death-final-r26-a`, `needler-death-final-r26-a`, `scuttler-death-final-r26-a` (png, gif), the capture `art-batch-capture-r26-a` | the Scuttler's lime glow is weak on Luna grey; fixed after the batch: a ground unit's burst and pieces now scroll with the ground, so they stay on the husk (also the Polyp Mortar's and other ground explosions) | |
| 2 | [Smart Bomb](../../player/specials/README.md) | production art from the chosen round-09 sheet (`tools/art/smart_bomb.py`): the ring's cross-section (`smart-bomb-ring`, 72×8, additive: halo, white core, trailing cyan band) drawn as 72 quads round the circle, so it stays crisp at every radius and dims over the last 30 % of its run; a 12-frame burst at the bomb point (128×128, 30 fps); the white field flash stays the spec's fill; its chosen sound wired by the SFX pass (choice 13) | `smart-bomb-final-r26-a` (png, gif), `art-batch-capture-r26-a` (Levels 02 and 04) | the bullets do not pop with a sparkle as the ring passes them, which the round-09 sheet shows (kept for later) | |
| 3 | [Boss bar plate](../../ui/hud/README.md#concept-art) | production art (`tools/art/boss_bar.py`, the HUD's metal look): `hud/boss-bar-plate`, a 64×20 nine-patch steel strip with riveted end caps and a recessed trough whose 6 px glass holds the red fill; stretched to 422 px round the act boss's 400 px bar and 262 px round the mid-boss's 240 px, at its own 20 px height | `boss-bar-plate-final-r26-a` | no game capture in this round: see it with `--level 5` (Gorgon Frigate) or `--level 7` (Brood Carrier); nothing moves, so no GIF | |
| 4 | [Act 1 title-card still](../../campaign/act-1-first-contact/README.md#concept-art) | production art (`tools/art/act_stills.py`): `ui/act-1-first-contact-still.png` (960×540), the Gagarin yards from above at dawn, composed from Level 01's approved backdrop pieces over Earth with the sunrise terminator, Lancer on the south launch rail, Aegis Two launching from the north arm, the middle kept open for the lettering; the briefing draws it darkened to 55 % behind the title card | `act-1-still-final-r26-a`, `art-batch-capture-r26-a` (the title card as the game draws it) | the right side is busy (the cruiser hull, the crane and dock frames); with this row approved Act 1's `art` becomes final | |
| 5 | [Ship: engine flames, damage smoke and sparks](../../player/ship/README.md) | the approved round-12 engine flames now drawn (additively under the hull at each banking frame's engine mounts, long while climbing, short while falling back); final damage sprites (`tools/art/ship_fx.py`): a smoke puff (16×16, dark violet-shadowed grey, 6 frames) trailing from between the engines below 30 % armour and denser below 15 %, and a spark burst (24×24, additive, white with a pale blue glow, no reserved hue) off one of six hull points below 15 % | `ship-damage-final-r26-a` (png, gif), `ship-visuals-capture-r26-a` | the smoke is regular (a puff every 4 steps, every 3 below 15 %); check that the trail does not hide bullets behind the ship and that the sparks do not read as incoming fire | |
| 6 | [Shield hex ring](../../player/shields/README.md#concept-art) | final shield-hit ring (`tools/art/ship_fx.py`, round 08's hex shimmer on a 60×60 round bubble, `ship-shield_0..3`, additive): the cells light up round the hit and a ripple runs back, over the 8-step shimmer with the hull's blue shimmer under it | `shield-ring-final-r26-a` (png, gif), `ship-visuals-capture-r26-a` | the hex cells are faint at 1×; the hit point is always at the front of the bubble (most bullets come from ahead), not at the bullet's side; the fade over 4 frames may be abrupt | |
| 7 | [Runtime drop shadows](../../art-direction/README.md#implementation) | built (`Shadows`): the shadow of every flyer, the Coilwyrm's segments and the ship from the sprite's alpha, dark at 50 %, 85 % scale, offset (21, 30) for air and (9, 13) for low-air, falling only on what catches shadows (the ground layer's tiles, road and pieces mark a stencil): on Level 04's regolith, on Level 03's wrecks but not on open space, on Level 01's girders but not on Earth below | `ship-visuals-capture-r26-a` | no 1–1.5 px blur and no second shadow on low-air bank tops (the art direction's two refinements); the stencil masking is verified only on llvmpipe (software GL under Xvfb), not on a real GPU | |
| 8 | [Medium enemy bullet](../../enemies/README.md#concept-art) | production art (`tools/art/bullet_medium.py`): the large pulsing orb for the `medium` class (`orb-medium_0..5`, 23×23, 13 px body, 6-frame pulse; round 09's large orb in the final small orb's colours), drawn for the Leviathan's and the Scuttler's aimed shots (before, every enemy bullet was drawn as the small orb); the Spore Bomber's `medium` is its spore, drawn as the mine | `enemy-bullet-medium-final-r26-a` (png, gif), `ship-visuals-capture-r26-a` (the Leviathan's orbs) | check that it reads as the same family but clearly heavier and that the pulse is calm; it plays the heavy shot sound (choice 13) | |
| 9 | [HUD warnings](../../ui/hud/README.md#decisions) | built, drawn in code in the round-09 look (no rendered art): **wave banners** ("WARNING — HOSTILES FROM THE REAR / LEFT / RIGHT", both sides, all sides) with each edge warning and its tone, one at a time, 2.5 s; the sensor suite's **threat arrows** from its L2 (D2 = A): amber arrowheads at the edge pointing at incoming air enemies off the screen, at most eight; the **low-armour flash** on the armour gauge at 30 % and faster at 15 %, in step with the beeps; the **credit numbers option** (Gameplay tab, `CREDIT NUMBERS`, default on). **Question:** approve the code-drawn look as final, or ask for rendered banner art? | `hud-warnings-capture-r26-a` | the simulation knows a unit only once it spawns 40 px outside its edge, so a wave's units show their arrows only about **0.3 s** before they enter (a Coilwyrm's body coming up from the rear 1–2 s); a warning that starts while a banner shows waits for it, or is dropped when its edge is no longer warned | |
| 10 | [Hangar test fire](../../ui/hangar/README.md#decisions) | built: the shop's 264×64 box loops the selected weapon at the shown level against three amber dummy targets (ahead, beside, behind), restarting with the sortie's own retry, deterministic. **Questions:** (a) the box shows the play field **turned a quarter clockwise** at half size (ahead is right, the ship's left up) so the wide box holds the field's length; keep that, or a different view? (b) **locked weapons** loop at L1, is that right? (c) with the UPGRADE choice highlighted the box shows **the next level** ("TEST FIRE L1>L2"), the purchase's result; keep? | `test-fire-capture-r26-a` | the Act 2 mines and torpedo say "NOT YET IN FLIGHT", other items "WEAPONS ONLY"; a dummy takes two level-1 hits, so a loop lasts at most about 8 s | |
| 11 | [Credits roll](../../ui/credits/README.md) | built (D5 = B): generated at build time from CREDITS.md (`:pipeline:credits` → `assets/ui/credits.txt`, checked by a test), scrolling at 30 px/s over the title scene, 8× faster while holding confirm, back at any time; today 23 CC-BY sound attributions, 54 thanked CC0 sound authors, 13 thanked LibriVox readers and the font with its licence. **Question:** confirm or replace the proposed own credit lines **"A GAME BY Emond Papegaaij"** and the tools line **"Written with Claude Code by Anthropic"** | `credits-screen-capture-r26-a` | reaching the roll after the final level waits for the campaign end (later: Act 7); the art and music lines ("made by the game's own generators") are proposals too | |
| 12 | [Music finals](../../audio/music/README.md#concept-art) | production files (`tools/art/themes.py`, D6 = A, "as they are"): #4 "Afterburner" and #5 "Coalition Rising" with their base stems, #21 "Contact Heavy" (the mini-boss sting) and the jingles #23 "Mission Complete", #25 "Mission Failed" and #26 "Game Over"; six identical to the chosen files with a `SOURCE` comment, all at −14 LUFS (stems at their full mix's gain), with four loop-seam listening aids | `afterburner-final-r26-a`, `afterburner-base-final-r26-a`, `coalition-rising-final-r26-a`, `coalition-rising-base-final-r26-a`, `miniboss-sting-final-r26-a`, `mission-complete-final-r26-a`, `mission-failed-final-r26-a`, `game-over-final-r26-a`, the seam aids `afterburner-seam-final-r26-a`, `afterburner-base-seam-final-r26-a`, `coalition-rising-seam-final-r26-a`, `coalition-rising-base-seam-final-r26-a`, the sheet `music-final-r26-a` | **"Afterburner" and its base stem got micro gain dips at 57 kick transients** (at most −1.1 dB, 3.1 s of 154.7 s, 2 ms attack, 50 ms release, both files alike so the crossfade stays exact) to meet the −1 dBTP true-peak limit (they were −0.5 and −0.8 dBTP); measured only, not listened to: **listen with the seam aids** and the full Afterburner for pumping | |
| 13 | [Newly wired SFX](../../audio/sfx/README.md#decisions) | the SFX pass: a 32-voice limit with priority stealing; chosen sounds now played (see [Listening](#listening), group SFX): the **shop** (buy, sell, denied, equip, upgrade), the **pickups** by type, **explosions by size** (two files a rung; set pieces, phase ends and boss chains on their rungs), ground targets crumbling, the **Smart Bomb** (its energy blast over the huge rung's boom at −3 dB), the **shield's restore chime** (once full after a break), the **low-armour beeps** (1.2 s at 30 %, 0.6 s at 15 %, 6 dB under the warnings) and the **heavy enemy shot** for `medium` bullets. **Questions:** which event should cue the Vrell screech (c, d: no Act 1 event has it yet)? A save-done sound and a Coilwyrm chain-cut tear have none chosen (the cut plays a lower Brood Pod burst): concepts in a later round? | the sound files under [Listening](#listening) | not listened to: the levels are set by rule; the beeps can last the rest of a level, since armour does not regenerate | |
| 14 | [Okafor's low-armour line](../../player/armor/README.md#decisions) | one campaign-wide line at 15 % armour, once per attempt, grim: *"Lancer, your hull won't take much more. Fly careful."* (4.5 s; `assets/voice/okafor/14b14dfe36a3.ogg`, first row of [Listening](#listening)). It queues **urgent**: it interrupts the line playing, the cut line replays after it, and the timed lines behind it start late, typically by 2–13 s, at worst 18 s (Level 03 from t=36.5). **Question:** is that acceptable, or should the cut line not replay? | the line under [Listening](#listening) | Whisper reads the dry take back word for word, but through radio filter b it hears **"howl" for "hull"**: listen for it | |
| 15 | [Varga's intel lines](#varga-intel-lines) | D4 = A: one text line per sensor level per level (none, L1, L2, L3), 28 lines, 21 new, not voiced; quoted under [Part H numbers and texts](#varga-intel-lines) | — | **Level 07's L2 and L3 are one short line each** ("Sacs first, then the core.", "Cut the lifeboat's tow cable."), as the panel has no room for more | |
| 16 | [Pickup magnet](../../player/systems/README.md#pickup-magnet) | the magnet's pull speeds **240 / 300 / 360 px/s** at L1 / L2 / L3 (an agent's proposal; the radius 72 / 108 / 144 px is the design's): every pickup within reach flies straight at the ship; a pickup at the edge is taken about 0.15 s later at L1, 0.3 s at L3. The row stays `draft` until accepted | — | not played by the user yet; the shop shows no RADIUS stat for it | |
| 17 | [Balance numbers](#balance) | accept the balance pass under [Part H numbers and texts](#balance): Act 1 on every difficulty, the Leviathan's bounty 190 → 113 with Level 03's `bounty_scale` 1.07, the Coilwyrm as an accepted exception, hard's economy accepted with only L13's Composite II short | — | the autopilot earns about 1.6–2.1 × the typical haul per level (it kills 90–99 %), so a real player banks less; the Leviathan's TTK is judged at 0.6 × DPS (accepted) | |
| 18 | [Voiced lines, Levels 01–06](#listening) | **listening row**: the 128 voiced lines no round has reviewed: the act briefing's 5 pages, Levels 01–06's 12 briefing pages and 110 radio lines, and Hammer Lead's Airstrike call; accept as rendered, or name the lines to re-render (a new seed or a pinned take) | the files under [Listening](#listening) | rendered 2026-10-03 to 10-05 with the cast voices, reviewed by Whisper only; listen for names (Aegis, Daedalus, Gagarin, Shackleton, Tranquility) and the shouted lines | |
| 19 | [Part H defaults and decisions](#part-h-numbers-and-texts) | accept the defaults listed under [Part H numbers and texts](#part-h-numbers-and-texts), and check that the user decisions listed there were carried out | — | — | |

## Part H numbers and texts

Chosen by the agents to close part H (choices 15–17 and 19); the details live in the parts'
READMEs (Decisions of 2026-10-05).

### Varga intel lines

The hangar's intel panel shows the line of the fitted sensor suite's level (easy adds 1); the
lines live in each level's `data.yaml` (`threat_profile.varga`).

| Level | No sensor | Sensor L1 | Sensor L2 | Sensor L3 |
|---|---|---|---|---|
| [01](../../campaign/act-1-first-contact/level-01-break-at-dawn/data.yaml) | "Our scans are patchy, Lancer. Small, fast, lots of them." | "Mostly head-on, some from the flanks, and one group may come up behind you. Light traffic. For now." | "Skitters ram, Needlers shoot. Needlers first. The last eight circle above you, then break at you one by one." | "Your cannon is enough; keep it forward. One cache: a crane beacon blinking mid-run. Three hits." |
| [02](../../campaign/act-1-first-contact/level-02-shipyard-burning/data.yaml) | "The growths on the hulls shoot back, Lancer. Once you're past them, they can't track you." | "A third of them come in from the sides. And Crane Four is still on automatic. Mind the arm when it sweeps." | "New one: the Stinger. It stops, glows red, then dives. Sidestep on the red. The growths are turrets, rooted on the hulls." | "Something wide for the flanks. One cache: a canister on Crane Four's hook. Hit the clamp while the arm sweeps." |
| [03](../../campaign/act-1-first-contact/level-03-spore-drift/data.yaml) | "Slow carriers in the high lanes and a lot of wreckage, Lancer. And something big on long range." | "All of it from ahead, through a debris field. The big chunks stop your shots and hurt to touch. Drifting mines too." | "Spore Bombers fly below you; their spores rise a second later. The big contact matches nothing on file. Out-fly it." | "Bring a spread. The big one is out of reach until it comes down. Cache: four amber lights on the Kestrel wreck." |
| [04](../../campaign/act-1-first-contact/level-04-tranquility-run/data.yaml) | "Walkers in the craters and egg sacs coming down, Lancer. And five crawlers that can't dodge." | "Nearly all from ahead, but walkers come over both crater rims. The ground fire goes for the crawlers, not you." | "Brood Pods hatch in eight seconds; kill them first. Scuttler claws are armour; hit the back. Airstrike is cleared." | "Bring something that hits the ground. Cache: a dugout in a crater rim, late on. Bombs or an Airstrike open it." |
| [05](../../campaign/act-1-first-contact/level-05-crater-nest/data.yaml) | "Acid mortars and turrets on a crater floor, Lancer. Something in orbit is watching." | "All four batteries, or the nest survives. Sleds run the rail when its lights are on. And wreckage floats down there." | "A Gorgon Frigate drops in last. Take the heads one at a time, then the core." | "Anti-ground for the batteries. Cache: a canister jammed on the rail. Hit it between sleds." |
| [06](../../campaign/act-1-first-contact/level-06-farside/data.yaml) | "The far side is dark, Lancer. Snipers at the edges, and something long that comes round behind you." | "Most come from ahead. The dangerous ones hold the edges or come up behind you. You can't shoot backwards yet." | "Mantis snipers sweep beams from the edges. Cut a Coilwyrm and the tail grows a head. Aim for the head." | "Fire sideways. A cache in a dark crater, hit only while lit, and a terminal at the gate." |
| [07](../../campaign/act-1-first-contact/level-07-brood-carrier/data.yaml) | "One ship at L1. Station-sized." | "Its escort comes head-on, with snipers at the edges. Then the carrier. Nothing else counts." | "Sacs first, then the core." | "Cut the lifeboat's tow cable." |

### Balance

From the [economy](../../systems/economy/README.md#decisions) and
[Level 03](../../campaign/act-1-first-contact/level-03-spore-drift/README.md#decisions):

- **Act 1 playthrough** (`ActPlaythroughTest`, the test autopilot with the balance plan's
  purchases, credits carried from level to level): every level won at the first attempt on every
  difficulty; Act 1 ends with **8,349 / 7,108 / 6,525** credits on easy / medium / hard. The plan's
  L14 visit buys one Airstrike charge instead of two (it would carry five, over the four it holds).
- **Leviathan** (user decision): bounty **190 → 113**, about 15 % of Level 03's budget (it was 23 %,
  set when the budget was 1,145); Level 03's `bounty_scale` **0.97 → 1.07**, so the typical haul is
  783 of the budget's 801 (−2 %, was 805), perfect 1,288 → 1,272.
- **Coilwyrm** (user decision): its bounty, 86 for its parts against the `large` class's 40–60,
  stays as an accepted exception (a multi-part enemy; cutting it up is extra work).
- **Hard Level 05** (user decision, option e): the test autopilot is fixed, not the game: it flies
  low while a battery unit is its target and picks its boss-fight moves by a 0.6 s look-ahead; it
  wins hard Level 05 at the first attempt on 20 of 20 seeds with the Pulse Cannon L2 (0 before),
  so the plan's hard-only Pulse Cannon L3 is removed.
- **Hard economy** (user decision): accepted as tighter; `BalanceTest` prints the hard sheet and
  its one shortfall as accepted: the L13 visit's Composite II plating, **425 credits short**.
- `tools/balance.py` exits 0, the Levels 01–03 DPS gap printed as the accepted exception.

### Defaults

Stated in the part's plan, not objected to:

- **Wave banners** derived from the waves' entry edge (no new data), with the existing edge
  warning and its tone; **credit numbers option** `gameplay.credit-numbers`, default on.
- **Low-armour line**: one Okafor line at 15 %, once per attempt, voiced (choice 14).
- **Test fire**: a live mini-sortie of the selected weapon at the shown level against three dummy
  targets, looping in the box (reuses the simulation, deterministic; choice 10).
- **SFX pass**: every chosen sound in `assets/sfx/` that an Act 1 event uses is played; the
  32-voice limit with priority stealing; the Act 2 sounds (Tail Gun, mines, torpedo) tagged
  `later: M5`.
- **Third utility bay**: a data entry with `available: act 3`.
- **Mission select**: its `review` design is approved when the user plays the build.
- **Balance gate (D1)**: the balance checks are JUnit tests (`BalanceTest`, `ActPlaythroughTest`),
  `tools/balance.py` stays the printed balancing sheet.

**User decisions of part H** (check they were carried out):

- [ ] **D2 = A**: the Pickup magnet's radius (and the pull speeds of choice 16) and the sensor
  suite's L2 threat arrows built; the Targeting computer out of the shop until M5 (`for_sale:
  false`), its L06 data-core unlock still recorded in the save.
- [ ] **D3 = A**: the per-bullet `medium+` / `hard-only` tags closed as met by the per-enemy
  `difficulty:` overrides and the waves' `skip`; the design line reworded
  ([difficulty](../../systems/difficulty/README.md), [enemies](../../enemies/README.md)).
- [ ] **D4 = A**: one text line per sensor level per level, 28 lines, 21 new, not voiced (choice 15).
- [ ] **D5 = B**: the credits screen built, generated from CREDITS.md (choice 11); the high-score
  table on the M6 row of the [roadmap](../../tech/roadmap/README.md).
- [ ] **D6 = A**: the six music files checked against the final spec, only "Afterburner" and its
  stem fixed (the true peak), offered as they are (choice 12).
- [ ] **Leviathan**: bounty 113, Level 03's `bounty_scale` 1.07.
- [ ] **Coilwyrm**: its bounty kept as an accepted exception.
- [ ] **Hard Level 05**: the autopilot fixed (option e), the plan's hard-only Pulse Cannon L3
  removed.
- [ ] **Hard economy**: accepted as tighter, the L13 Composite II shortfall printed as accepted.

## Listening

Okafor's low-armour line (choice 14), the newly wired sounds (choice 13) and the 128 voiced lines
of the act briefing and Levels 01–06 (choice 18), from `assets/voice/` and the SFX's `concept/`.
Briefing pages are quoted by their first sentence.

| Group | Cue | Speaker | Text or sound | File |
|---|---|---|---|---|
| Armour | 15 % armour, any level | Okafor | "Lancer, your hull won't take much more. Fly careful." (listen for "hull") | [play](../../../assets/voice/okafor/14b14dfe36a3.ogg) |
| SFX | Shop: buy | — | blip and coin sparkle | [play](../../audio/sfx/concept/ui-shop-buy-r08-a.ogg) |
| SFX | Shop: sell, refund, undo | — | three descending coin blips | [play](../../audio/sfx/concept/ui-shop-sell-r08-a.ogg) |
| SFX | Shop: denied | — | low beating square buzz | [play](../../audio/sfx/concept/ui-shop-denied-r08-a.ogg) |
| SFX | Shop: equip | — | equip | [play](../../audio/sfx/concept/ui-equip-r08-a.ogg) |
| SFX | Shop: upgrade | — | upgrade | [play](../../audio/sfx/concept/ui-upgrade-r08-a.ogg) |
| SFX | Pickup: salvage S | — | salvage small | [play](../../audio/sfx/concept/pickup-salvage-small-r08-a.ogg) |
| SFX | Pickup: salvage M | — | salvage medium | [play](../../audio/sfx/concept/pickup-r01-c.ogg) |
| SFX | Pickup: salvage L | — | salvage large | [play](../../audio/sfx/concept/pickup-salvage-large-r08-a.ogg) |
| SFX | Pickup: shield cell | — | shield cell | [play](../../audio/sfx/concept/pickup-shield-cell-r08-a.ogg) |
| SFX | Pickup: armour patch | — | armour patch | [play](../../audio/sfx/concept/pickup-armour-patch-r08-a.ogg) |
| SFX | Pickup: special charge | — | its own chime | [play](../../audio/sfx/concept/pickup-special-charge-r08-a.ogg) |
| SFX | Pickup: overdrive | — | with the overdrive's start cue | [play](../../audio/sfx/concept/pickup-r01-a.ogg) |
| SFX | Pickup: data core | — | data core | [play](../../audio/sfx/concept/pickup-r01-b.ogg) |
| SFX | Explosion `small` a, b | — | small rung (the pair alternates) | [a](../../audio/sfx/concept/explosion-r02-a.ogg), [b](../../audio/sfx/concept/explosion-small-r03-a.ogg) |
| SFX | Explosion `medium` a, b | — | Brood Pod, Mantis, Scuttler, Spore Bomber; set-piece parts; boss chains' bursts | [a](../../audio/sfx/concept/explosion-r02-c.ogg), [b](../../audio/sfx/concept/explosion-medium-r03-b.ogg) |
| SFX | Explosion `large` a, b, boss blast | — | mid-boss deaths, phase ends, the start of a boss chain; the boss blast 0.05 s after its end | [a](../../audio/sfx/concept/explosion-r02-d.ogg), [b](../../audio/sfx/concept/explosion-large-r03-a.ogg), [boss](../../audio/sfx/concept/explosion-r02-e.ogg) |
| SFX | Explosion `huge` a, b | — | act-boss deaths, the Leviathan; the boom under the Smart Bomb | [a](../../audio/sfx/concept/explosion-huge-r04-a.ogg), [b](../../audio/sfx/concept/explosion-huge-r03-b.ogg) |
| SFX | Ground target burst | — | small burst with a crumble: collapse for 1,000 px² and more, rubble below | [collapse](../../audio/sfx/concept/hit-crumble-r08-a.ogg), [rubble](../../audio/sfx/concept/hit-crumble-r08-b.ogg) |
| SFX | Smart Bomb | — | charge-up and white-out boom (with the huge b boom at −3 dB) | [play](../../audio/sfx/concept/special-smartbomb-r08-a.ogg) |
| SFX | Shield restored | — | rising chime, once full after a break | [play](../../audio/sfx/concept/player-shield-restore-r08-a.ogg) |
| SFX | Low armour | — | beeps, every 1.2 s at 30 %, 0.6 s at 15 % | [play](../../audio/sfx/concept/player-low-armour-r08-a.ogg) |
| SFX | Heavy enemy shot a, b | — | the `medium` bullets (Leviathan, Scuttler) | [a](../../audio/sfx/concept/enemy-shot-heavy-r08-a.ogg), [b](../../audio/sfx/concept/enemy-shot-heavy-r08-b.ogg) |
| SFX | Vrell screech c, d (question) | — | no Act 1 event cues it yet | [c](../../audio/sfx/concept/enemy-screech-r08-c.ogg), [d](../../audio/sfx/concept/enemy-screech-r08-d.ogg) |
| Act 1 | Act briefing page 1 | Okafor | "On the fourteenth of March, the Tether Gate lit up…" | [play](../../../assets/voice/okafor/eb72d109ac73.ogg) |
| Act 1 | Act briefing page 2 | Okafor | "Eleven days later our outer stations stopped answering…" | [play](../../../assets/voice/okafor/5bbe8ddb8675.ogg) |
| Act 1 | Act briefing page 3 | Okafor | "This morning a Vrell strike group came out of nowhere at the Earth–Moon L1 point…" | [play](../../../assets/voice/okafor/d0f56a8270e6.ogg) |
| Act 1 | Act briefing page 4 | Okafor | "The Defence Force was built to chase pirates…" | [play](../../../assets/voice/okafor/80f88d105d85.ogg) |
| Act 1 | Act briefing page 5 | Okafor | "Lancer, you've had the Stormhawk for nine days…" | [play](../../../assets/voice/okafor/9711ed2ce94e.ogg) |
| Specials | Airstrike call | Hammer Lead | "Hammer flight, inbound!" | [play](../../../assets/voice/hammer-lead/720c7eeba1e4.ogg) |
| L01 | Briefing page 1 | Okafor | "Lancer, this is Aegis Actual…" | [play](../../../assets/voice/okafor/f1b34f138d04.ogg) |
| L01 | Briefing page 2 | Varga | "Their ships don't show up as metal on our scopes…" | [play](../../../assets/voice/varga/0821ed664ea2.ogg) |
| L01 | t=2 | Rook | "Lancer, Rook. Aegis Two's got the north arm, you've got the south. Try not to have all the fun." | [play](../../../assets/voice/rook/629e5ccd082f.ogg) |
| L01 | t=18 | Okafor | "Contacts inbound. Weapons free." | [play](../../../assets/voice/okafor/08c0ebe449b7.ogg) |
| L01 | First kill | Rook | "They pop like bugs. Big, angry bugs." | [play](../../../assets/voice/rook/9f5ffc435f99.ogg) |
| L01 | t=40 | Varga | "Those ones are armed. Keep moving." | [play](../../../assets/voice/varga/f23fc41334f6.ogg) |
| L01 | t=73 | Rook | "Movement on your left! They're flanking the yard." | [play](../../../assets/voice/rook/1c0b97f8baee.ogg) |
| L01 | t=131 | Okafor | "Contacts on your six, Lancer!" | [play](../../../assets/voice/okafor/95d3726dafd4.ogg) |
| L01 | t=131, easy | Okafor | "More contacts, dead ahead!" | [play](../../../assets/voice/okafor/94c29e355225.ogg) |
| L01 | t=161 | Varga | "That isn't noise. There's structure in it. Commander, I think that was a signal." | [play](../../../assets/voice/varga/a94947b69199.ogg) |
| L01 | Level end | Okafor | "Good work, Aegis. That was the scouts. The rest are coming." | [play](../../../assets/voice/okafor/6f55a8279849.ogg) |
| L01 | Secondary met | Okafor | "Clean sweep. I'll make sure High Command hears about it." | [play](../../../assets/voice/okafor/3109ed4c2c2a.ogg) |
| L01 | Secret: beacon cache | Rook | "Nice shooting. Finders keepers." | [play](../../../assets/voice/rook/548278898989.ogg) |
| L02 | Briefing page 1 | Okafor | "Lancer, the scouts were the knock on the door…" | [play](../../../assets/voice/okafor/0ce5c1a3854d.ogg) |
| L02 | Briefing page 2 | Varga | "About yesterday's transmission…" | [play](../../../assets/voice/varga/0d3d2de1c84e.ogg) |
| L02 | t=0 | Okafor | "Aegis, the yards are burning. Four docks still have crews aboard. Clear those hulls." | [play](../../../assets/voice/okafor/07aa5cd6aca2.ogg) |
| L02 | t=11.3 | Rook | "Aegis Two on the north arm. It's a mess over here too, Lancer. Don't wait for me." | [play](../../../assets/voice/rook/5d977e5402ba.ogg) |
| L02 | t=22.5 | Varga | "That growth on the platform is alive. It's a turret. It *grew* a turret, Commander." | [play](../../../assets/voice/varga/c4cb3088d9b5.ogg) |
| L02 | t=34 | Dock One | "Dock One to anyone! They're growing on the hull — burn it off us!" | [play](../../../assets/voice/dock/5aa4ee6fc26d.ogg) |
| L02 | Dock cleared (any of the four) | Dock One | "We're clear! Crew's moving to the shuttles. Thank you, Aegis!" | [play](../../../assets/voice/dock/2be3632b7aae.ogg) |
| L02 | Dock lost | Yard Control | "Dock One is gone. We lost contact with the crew." | [play](../../../assets/voice/yard-control/cb831dc3eb8d.ogg) |
| L02 | Dock lost | Yard Control | "Dock Two is gone. We lost contact with the crew." | [play](../../../assets/voice/yard-control/688a1d199aa6.ogg) |
| L02 | Dock lost | Yard Control | "Dock Three is gone. We lost contact with the crew." | [play](../../../assets/voice/yard-control/23e85b724f40.ogg) |
| L02 | Dock lost | Yard Control | "Dock Four is gone. We lost contact with the crew." | [play](../../../assets/voice/yard-control/3647987dbe0c.ogg) |
| L02 | First dock lost | Okafor | "Keep moving, Lancer. The others still need you." | [play](../../../assets/voice/okafor/a5cc30f833df.ogg) |
| L02 | t=49 | Varga | "Fast one inbound. It stops before it dives. When it glows red, move sideways." | [play](../../../assets/voice/varga/0590626d1c1b.ogg) |
| L02 | t=78 | Yard Control | "Crane Four is still on automatic. Watch the arm, Aegis!" | [play](../../../assets/voice/yard-control/27604603a9c1.ogg) |
| L02 | t=106 | Rook | "Bandits coming in on your right flank, Lancer!" | [play](../../../assets/voice/rook/e540586e2797.ogg) |
| L02 | t=116 | Rook | "Both edges! Pick a side and make it count." | [play](../../../assets/voice/rook/e9ff56ab1b27.ogg) |
| L02 | t=138 | Dock Four | "Coolant breach on the *Resolute*! You'll be flying through it!" | [play](../../../assets/voice/dock/fc39263bd74f.ogg) |
| L02 | t=160 | Rook | "You know, they told me you were the quiet type. Good. More airtime for me." | [play](../../../assets/voice/rook/89cf61b04352.ogg) |
| L02 | Level end | Okafor | "That's the yards. Not all of them. Enough. Come home, Aegis." | [play](../../../assets/voice/okafor/d3b9874e85d6.ogg) |
| L02 | Secondary met | Okafor | "Four docks, four crews. Well flown, Lancer." | [play](../../../assets/voice/okafor/c3d9342953ff.ogg) |
| L02 | Secret: crane cache | Rook | "Supply drop off the hook. I'll pretend I didn't see that." | [play](../../../assets/voice/rook/7e33d6e7e9f4.ogg) |
| L03 | Briefing page 1 | Okafor | "Lancer, the yards are holding…" | [play](../../../assets/voice/okafor/ce0ff266b021.ogg) |
| L03 | Briefing page 2 | Varga | "They're spore bombers, gas-bags flying below your level…" | [play](../../../assets/voice/varga/90f88da384c2.ogg) |
| L03 | t=1 | Okafor | "Aegis, the high lanes are filling up with something. Find out what, and stop it." | [play](../../../assets/voice/okafor/0376ce3af693.ogg) |
| L03 | t=12 | Varga | "That gas-bag is full of spores. They float up to your level after a second. Shoot them once they glow." | [play](../../../assets/voice/varga/6836288cc04a.ogg) |
| L03 | t=24 | Rook | "Aegis Two in the wreck field north of you. Watch the big chunks, Lancer. They don't care whose side you're on." | [play](../../../assets/voice/rook/e1cea2311f83.ogg) |
| L03 | t=36.5 | Ring Control | "Debris field ahead. Big pieces will stop your rounds. And theirs." | [play](../../../assets/voice/ring-control/f3d6d2890d60.ogg) |
| L03 | Spore Bomber escaped | Okafor | "One got past. That's spores on somebody's city, Aegis." | [play](../../../assets/voice/okafor/da605f22c77c.ogg) |
| L03 | t=47 | Varga | "Commander, something very large on long range. It's not on any chart. And it's… singing?" | [play](../../../assets/voice/varga/25fc3ceb4639.ogg) |
| L03 | t=64.5 | Rook | "Okay. Okay. That's big. We can do big." | [play](../../../assets/voice/rook/76c802d3a39d.ogg) |
| L03 | t=71 | Varga | "It's above you. Your guns can't reach that high. Dodge the seeds and wait." | [play](../../../assets/voice/varga/fcc5d8b79ea4.ogg) |
| L03 | t=126 | Varga | "It's coming down to your level! The vents on its back, the fins, the blowhole. Hit the glow." | [play](../../../assets/voice/varga/b4bb94930ad0.ogg) |
| L03 | Leviathan killed | Ring Control | "Ring Control confirms: the big contact is down." | [play](../../../assets/voice/ring-control/6c9a2fca611d.ogg) |
| L03 | Leviathan escaped | Varga | "It's leaving. I have a feeling we'll see that one again." | [play](../../../assets/voice/varga/981fdcb31c6e.ogg) |
| L03 | t=168.5 | Okafor | "Good flying. And Lancer, High Command just gave us Hammer flight: two bombers on call. You'll find them in the hangar." | [play](../../../assets/voice/okafor/5817459e192f.ogg) |
| L03 | Level end | Okafor | "Lane's clear. Come home, Aegis." | [play](../../../assets/voice/okafor/65c11bde647d.ogg) |
| L03 | Secondary met | Okafor | "Not one bomber got through. Earth owes you a drink, Lancer." | [play](../../../assets/voice/okafor/162506dc9514.ogg) |
| L03 | Secret: lifeboat rack | Rook | "The *Kestrel*'s lifeboat stores. Her crew would want you to have them, Lancer." | [play](../../../assets/voice/rook/2645f6ddc100.ogg) |
| L04 | Briefing page 1 | Okafor | "Lancer, brood pods came down around Tranquility Base overnight…" | [play](../../../assets/voice/okafor/47b88f56a32a.ogg) |
| L04 | Briefing page 2 | Varga | "The walkers turn to face where they're going…" | [play](../../../assets/voice/varga/f9889f322980.ogg) |
| L04 | t=0 | Okafor | "Five crawlers, four hundred civilians. Get them to the terminal, Lancer." | [play](../../../assets/voice/okafor/37789633f9a9.ogg) |
| L04 | t=11 | Crawler One | "Crawler One rolling. We're slow and we're loud, Aegis. Please don't leave us." | [play](../../../assets/voice/crawler-one/eaecc7017ca0.ogg) |
| L04 | t=22 | Varga | "That's an egg sac. It hatches on its own in eight seconds. Kill it before then, and be ready for what comes out." | [play](../../../assets/voice/varga/b4babe6dbbfc.ogg) |
| L04 | t=34.5 | Rook | "Aegis Two over Shackleton. My family's down there somewhere, Lancer. Bring those crawlers home." | [play](../../../assets/voice/rook/2b211c9b2b3d.ogg) |
| L04 | t=60 | Okafor | "Hammer flight is on station. Your call, Lancer." | [play](../../../assets/voice/okafor/94cba7c44135.ogg) |
| L04 | First crawler hit | Convoy | "We're taking fire! Crawler One is hit!" | [play](../../../assets/voice/convoy/7a76ad352299.ogg) |
| L04 | First crawler hit | Convoy | "We're taking fire! Crawler Two is hit!" | [play](../../../assets/voice/convoy/23aba3ac820a.ogg) |
| L04 | First crawler hit | Convoy | "We're taking fire! Crawler Three is hit!" | [play](../../../assets/voice/convoy/ac759c835b71.ogg) |
| L04 | First crawler hit | Convoy | "We're taking fire! Crawler Four is hit!" | [play](../../../assets/voice/convoy/47f3e11f0af0.ogg) |
| L04 | First crawler hit | Convoy | "We're taking fire! Crawler Five is hit!" | [play](../../../assets/voice/convoy/e7afef92dbfe.ogg) |
| L04 | First crawler lost | Crawler One | "We lost One. Oh God, we lost One." | [play](../../../assets/voice/crawler-one/59cdc74cad36.ogg) |
| L04 | First crawler lost | Crawler One | "We lost Two. Oh God, we lost Two." | [play](../../../assets/voice/crawler-one/68df219ca8e1.ogg) |
| L04 | First crawler lost | Crawler One | "We lost Three. Oh God, we lost Three." | [play](../../../assets/voice/crawler-one/971a04db0a82.ogg) |
| L04 | First crawler lost | Crawler One | "We lost Four. Oh God, we lost Four." | [play](../../../assets/voice/crawler-one/7729b2df4445.ogg) |
| L04 | First crawler lost | Crawler One | "We lost Five. Oh God, we lost Five." | [play](../../../assets/voice/crawler-one/4b16a795a668.ogg) |
| L04 | First crawler lost, after | Okafor | "Keep the rest moving, Lancer." | [play](../../../assets/voice/okafor/a0a2faeb40a8.ogg) |
| L04 | t=74 | Varga | "Walker on the rille rim. The claws are armour. Wait for it to turn, then hit the glowing back." | [play](../../../assets/voice/varga/e21a6ae5cf83.ogg) |
| L04 | t=86 | Tranquility Control | "Walkers coming over the crater rims, both sides of the road!" | [play](../../../assets/voice/tranquility-control/6b0839bbcc25.ogg) |
| L04 | t=128 | Tranquility Control | "Lander impact, grid four. Dust everywhere. Stay on the convoy." | [play](../../../assets/voice/tranquility-control/2c1338330045.ogg) |
| L04 | t=176 | Crawler One | "Terminal in sight! Open those doors!" | [play](../../../assets/voice/crawler-one/924072cf2db8.ogg) |
| L04 | Level end, 5 crawlers | Okafor | "All five. Four hundred people. That's what we're for, Aegis." | [play](../../../assets/voice/okafor/6eeb914e9328.ogg) |
| L04 | Level end, 1–4 crawlers | Okafor | "We got most of them home. Most." | [play](../../../assets/voice/okafor/748f08118bee.ogg) |
| L04 | Mission failed | Okafor | "The convoy is gone, Lancer. Pull back." | [play](../../../assets/voice/okafor/7c31645c731c.ogg) |
| L04 | Secondary met | Varga | "Not one sac hatched on its own. You're learning their rhythm." | [play](../../../assets/voice/varga/a3b22900b38f.ogg) |
| L04 | Secret: prospector's cache | Okafor | "A prospector's cache. Whoever dug in there would want it used, Lancer." | [play](../../../assets/voice/okafor/eaebac4b8b86.ogg) |
| L05 | Briefing page 1 | Okafor | "Lancer, the pods at Tranquility weren't random…" | [play](../../../assets/voice/okafor/781043d8fc05.ogg) |
| L05 | Briefing page 2 | Varga | "The acid-throwers mark their target a second before it lands…" | [play](../../../assets/voice/varga/73c21096e8b7.ogg) |
| L05 | t=1 | Okafor | "There's the crater, Aegis. Four batteries on the floor. All four die, or this was for nothing." | [play](../../../assets/voice/okafor/3b4117f0fa37.ogg) |
| L05 | t=12.5 | Driver Control | "Mass driver's still on automatic, Aegis. Sleds every five seconds. Watch the rail lights." | [play](../../../assets/voice/driver-control/093687b550f9.ogg) |
| L05 | t=27 | Varga | "That one lobs acid. It marks where it'll land, a second ahead. Don't be there." | [play](../../../assets/voice/varga/b02b32193f03.ogg) |
| L05 | t=46 | Rook | "Aegis Two here. Two groups, both flanks of you, Lancer!" | [play](../../../assets/voice/rook/8a06d3147ff5.ogg) |
| L05 | Battery cleared | Okafor | "Battery down." | [play](../../../assets/voice/okafor/39f573b89039.ogg) |
| L05 | Battery cleared | Okafor | "Two." | [play](../../../assets/voice/okafor/363f2308302e.ogg) |
| L05 | Battery cleared | Okafor | "Three. One left." | [play](../../../assets/voice/okafor/fe800558866d.ogg) |
| L05 | Battery cleared | Okafor | "That's the last battery. The nest is open." | [play](../../../assets/voice/okafor/4c7d5c7267eb.ogg) |
| L05 | Battery passed (mission failed) | Okafor | "Battery A is behind you. The nest survives. Pull back, Lancer." | [play](../../../assets/voice/okafor/505b891a6149.ogg) |
| L05 | Battery passed (mission failed) | Okafor | "Battery B is behind you. The nest survives. Pull back, Lancer." | [play](../../../assets/voice/okafor/ae8dbd2f0878.ogg) |
| L05 | Battery passed (mission failed) | Okafor | "Battery C is behind you. The nest survives. Pull back, Lancer." | [play](../../../assets/voice/okafor/edaeadfb7302.ogg) |
| L05 | Battery passed (mission failed) | Okafor | "Battery D is behind you. The nest survives. Pull back, Lancer." | [play](../../../assets/voice/okafor/f948c27d4dc0.ogg) |
| L05 | t=116.5 | Driver Control | "The nest is venting. Dust everywhere. Your scopes will clear in a few seconds." | [play](../../../assets/voice/driver-control/4df3f7ae0826.ogg) |
| L05 | t=150.5 | Driver Control | "Big contact dropping out of orbit, right on top of the crater!" | [play](../../../assets/voice/driver-control/ab612a3c12b7.ogg) |
| L05 | t=158 | Varga | "A warship. Three heads, all guns. Take the heads one at a time; then the crown opens. Hit the core." | [play](../../../assets/voice/varga/cce08e41c3e8.ogg) |
| L05 | t=170 | Rook | "Oh, it has *heads*. Of course it has heads." | [play](../../../assets/voice/rook/08295e09d699.ogg) |
| L05 | Boss phase (core) | Varga | "Crown's opening. That's the core. Lime glow, Lancer!" | [play](../../../assets/voice/varga/c2376ae4f33e.ogg) |
| L05 | Boss destroyed | Okafor | "Frigate down. The nest is dead. Get out of there, Aegis." | [play](../../../assets/voice/okafor/de2fcf29f275.ogg) |
| L05 | Level end | Okafor | "Good work. Varga wants samples. I told her no." | [play](../../../assets/voice/okafor/e92ae98c8897.ogg) |
| L05 | Secondary met | Varga | "Every growth in that crater burned. We'll have nothing to study. Well done, I suppose." | [play](../../../assets/voice/varga/e30ae4a3596a.ogg) |
| L05 | Secret: stuck sled | Rook | "That canister sat jammed on the rail all week. Finders keepers, Lancer." | [play](../../../assets/voice/rook/7c2ba56882e8.ogg) |
| L06 | Briefing page 1 | Okafor | "Lancer, three farside settlements stopped answering in the last thirty hours…" | [play](../../../assets/voice/okafor/7040235f7588.ogg) |
| L06 | Briefing page 2 | Varga | "Two new contacts…" | [play](../../../assets/voice/varga/da6856e2d003.ogg) |
| L06 | t=2 | Okafor | "Aegis, Daedalus Rim stopped answering thirty hours ago. Find out why." | [play](../../../assets/voice/okafor/1081b8baf960.ogg) |
| L06 | t=12.5 | Rook | "Aegis Two on the north lane. I've got cousins on Daedalus, Lancer." | [play](../../../assets/voice/rook/fbe570b58c2d.ogg) |
| L06 | t=20 | Varga | "You're over the terminator. Headlight on. The Vrell glow; the ground doesn't. Trust the glow." | [play](../../../assets/voice/varga/4f7f6be9daaa.ogg) |
| L06 | t=32.5 | Rook | "Something on your left edge. It's just... sitting there." | [play](../../../assets/voice/rook/4310490f9091.ogg) |
| L06 | t=40 | Varga | "It's a sniper. It holds at the edge and sweeps a beam across you. Get an angle on it, or get away from that side." | [play](../../../assets/voice/varga/54cafdb4b849.ogg) |
| L06 | t=52.5 | Perimeter beacon | "...Daedalus perimeter. All residents report to shelter... all residents report..." | [play](../../../assets/voice/perimeter-beacon/a9aa82aed0ff.ogg) |
| L06 | t=64 | Okafor | "Domes intact. Airlocks open. Nobody's home." | [play](../../../assets/voice/okafor/d8b247b513ee.ogg) |
| L06 | t=76 | Varga | "Long contact, moving like a snake. Cut it and the back half grows a new head. Go for the head." | [play](../../../assets/voice/varga/414c1430b630.ogg) |
| L06 | t=112 | Rook | "It's turning, it's coming round behind you! Six, Lancer, six!" | [play](../../../assets/voice/rook/2fa10b4b73f7.ogg) |
| L06 | t=119.5 | Varga | "You can't shoot behind you yet. Dodge it." | [play](../../../assets/voice/varga/c4e8d153574b.ogg) |
| L06 | t=131 | Rook | "Contacts on six! Why is it always six?" | [play](../../../assets/voice/rook/67ac59e35230.ogg) |
| L06 | t=160 | Varga | "No bodies. No damage. Commander, the Vrell didn't kill them. They took them." | [play](../../../assets/voice/varga/252642d14aa4.ogg) |
| L06 | t=171 | Okafor | "...Understood. Log it, Varga. Aegis, finish this and come home." | [play](../../../assets/voice/okafor/cb91484efe6f.ogg) |
| L06 | t=180.5 | Rook | "Another one's coming round. Watch your tail." | [play](../../../assets/voice/rook/78d32c58a4bc.ogg) |
| L06 | Level end | Okafor | "Daedalus is empty. So are the others. We'll find them." | [play](../../../assets/voice/okafor/f72805311bab.ogg) |
| L06 | Level end, after | Rook | "...Copy. Aegis Two, heading home." | [play](../../../assets/voice/rook/3ca60bf717c0.ogg) |
| L06 | Secondary met | Okafor | "Not one sniper left standing. Good eyes in the dark, Lancer." | [play](../../../assets/voice/okafor/a906a1174fd4.ogg) |
| L06 | Secret: survey cache | Rook | "A CDF survey cache. Somebody meant to come back for that. Finders keepers, Lancer." | [play](../../../assets/voice/rook/4bd8ada434bc.ogg) |
| L06 | Secret: settlement log | Varga | "That's the settlement log. I'll... read it later." | [play](../../../assets/voice/varga/4b913e275992.ogg) |

## Notes

- Review file names: `<subject>-final-r26-a.png/.gif/.ogg` (production art and music) and
  `<subject>-capture-r26-a.png` (game captures) in each part's `concept/`; their `prompts.md`
  entries say how they are made. Nothing in `assets/` is hand-edited.
- No a/b variants in this round: every art row is *approve as final* or *redo*.
- The 128 voice files of choice 18 are the act briefing, Levels 01–06 and the Airstrike call as
  `pipeline/build/voice/lines.json` lists them; Level 07's lines and the outro pages were accepted
  in round 25.
- After the choices: the approved parts get `art: final` (with all art rows approved, Act 1 too),
  the production plan's part H item is ticked, the voice item "Act 1 lines … reviewed" is ticked,
  the magnet's row leaves `draft`, the credit lines are kept or replaced; then the user's
  playthrough of Act 1 closes M4.

## Decisions

- 2026-10-05: Opened with M4 part H.
