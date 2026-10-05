---
title: Concept round 23 — M4 part F, Level 06
design: approved
implementation: n/a
art: chosen
depends-on: [../../campaign/act-1-first-contact/level-06-farside]
updated: 2026-10-05
---

# Concept round 23 — M4 part F, Level 06

## Summary

The production-art round of M4 part F ([roadmap](../../tech/roadmap/README.md#m4-parts)): Level
06's Mantis and Coilwyrm, the darkness's glow frames, flare and light shapes, the far-side backdrop
and the two new intel portraits, rendered as **final** art by the generators in
[tools/art/](../../../tools/art/README.md) into `assets/`; a/b concepts for the parts no chosen
concept covers (user decision D8: the Mantis beam and telegraph, the ore cart, the survey cache, the
data core terminal, the new sounds); the perimeter beacon's voice audition through a new
public-address filter (user decision D7); a game capture of the level; and the numbers the agents
chose to close part F. Open [index.html](index.html) in a browser (regenerate with
`python3 tools/concept/board.py 23`). Every art choice is *approve as final* or *redo* (say what to
change); the concepts, sounds and voices are *pick a or b*; only an approved part gets `art: final`.
Level 06 is playable with `./gradlew :desktop:run --args="--level 6"`.

## Choices

| # | Part | What is proposed | Review files | Weak spots (Claude) | Outcome |
|---|---|---|---|---|---|
| 1 | [Mantis](../../enemies/air/mantis/README.md) | production art of the chosen r04 a model (`tools/art/mantis.py`): 80×80, nose down leaning into the field, its own render per edge (nothing mirrored): hover wing beat (4), telegraph with the arms opening and the eye charging (4), sweep (2), exit leaning out (4); the death's crimson flash and bone shards, wings and arms (12 frames each) | `mantis-final-r23-a` (png, gif) | the data says "facing inward, mirrored per side": it hangs nose down and leans 20–30° in instead, so the 40×56 hit box still fits; the beam starts at the unit's centre (the sim's origin), about 30 px above the eye; the wings flap as flat plates || approved — final, the beam moved to its head: beam, telegraph and aim start at the eye (`sweep.origin` in the Mantis data), see 6 |
| 2 | [Coilwyrm](../../enemies/air/coilwyrm/README.md) | production art of the chosen r08 a models (`tools/art/coilwyrm.py`): head (58 px, 4 jaw frames), regrown head (paler, wet), the 12 segment sizes of the taper (54 to 27 px) and the tail (40 px), **48 headings** each (the concept's 16 showed as kinks along a curve); the head's death (teal flash, skull, horns, mandibles), the segments' pop glint and pieces, the tail's pieces | `coilwyrm-final-r23-a` (png, gif) | **at the data's spacing (0.9 × the mean sprite size, a chain of about 480 px) the round bodies do not overlap: the chain reads as beads with gaps**; the concept used 0.5 (about 330 px, the spec's 20–30 % overlap); the sheet shows both: say which spacing (a data change) || accepted at spacing **0.5** (overlap, a chain of about 330 px; the Coilwyrm data changed); the regrowth sound is 12 |
| 3 | [Darkness](../../campaign/act-1-first-contact/level-06-farside/README.md#darkness-rules) | derived art for D2 (`tools/art/l06_darkness.py`): the Spine Turret's and Polyp Mortar's **glow frames** (their production models with only the emission left, indexed like their frames, a stepped halo; additive after the light pass), the **flare shell** (4 flicker frames), the **flare pool** (240×240) and **headlight cone** (232×200) as dithered light-map shapes | `darkness-final-r23-a` (png, gif) | the mortar glows as a whole lime bulb, not only its mouth; the turret's barrel tip is a 1 px dot; the review's ground is a stand-in, not the level's tiles; the dome and rail-lamp pools are still the code's smooth disc || approved — final |
| 4 | [Level 06 backdrop](../../campaign/act-1-first-contact/level-06-farside/README.md#layout) | production art (`tools/art/backdrop_l06.py`): four lamp-lit ground tile sets on one terrain and palette (terminator with sunlit crater rims, dome field, array field, Vrell field; the ore rail at x 410 through all), darkened plumes and dimmed ejecta, Daedalus Rim's domes with open airlocks, an overgrown and a broken dome, two Vrell nests, the dish arrays, the survey cache's dark crater, the landing pad and Daedalus Gate, rail lamps; 4.2 M px, no deep layer; the sheet shows each scene raw and under a light-map preview | `backdrop-final-r23-a` | **not yet in the game**: the level's `data.yaml` still says `images: level-04`; the block is in `backdrop-proposal.yaml` for the main agent to merge, with suggested lamp lights and target positions (the ore cart moves to the rail at x 410); the light map darkens the Vrell tendrils' teal and the terminator's sunlit rims too; the boulders keep Level 04's low-sun shadows || approved — final |
| 5 | [Intel portraits](../../ui/hangar/README.md) | production art (`tools/art/intel.py`): the Mantis (nose down) and the Coilwyrm (the head with its first segment) as 30×30 sensor-L2 portraits | `intel-final-r23-a` | the Coilwyrm's segment touches the plate's top edge; no boss silhouette (Level 06 has none) || approved — final |
| 6 | [Mantis beam and telegraph](../../enemies/air/mantis/README.md#concept-art) | **concepts**, pick a or b (`tools/concept/props_r23.py`): a *hot wire* (16×12 beam strip, tip and eye flares; the telegraph as today's dotted arc and dashed edges) or b *charged lance* (32×10 strip with energy knots running out, a tip spark, an eye ring; the telegraph one filled additive wedge, 302×348 for 70°, 302×428 for hard's 90°) | `mantis-beam-r23-a`, `-b` (png, gif) | the context views show the concept model turned in the model, not the production sprite, its wings cut at the field edge || **b** (charged lance) chosen, production art `tools/art/mantis_beam.py` drawn by the game; a moved to `concept/rejected/` |
| 7 | [Ore cart](../../campaign/act-1-first-contact/level-06-farside/README.md#secrets-and-pickups) | **concepts**, pick a or b: a open rust ore tub heaped with ore, hazard rim; b closed hopper with striped hatches and a red beacon (28×40, intact, damaged, wrecked) | `ore-cart-r23-a`, `-b` | the size is the concept agent's proposal; it must match the data's ground target || **b** (closed hopper) chosen; production sprites `tools/art/l06_props.py` (intact, damaged, wrecked, break-apart), review `l06-props-final-r23-a`; a moved to `concept/rejected/` |
| 8 | [Survey cache](../../campaign/act-1-first-contact/level-06-farside/README.md#secrets-and-pickups) | **concepts**, pick a or b: a ribbed olive CDF case whose three retro markers glint only while lit; b half-buried drum with three dim amber blinkers (a hint in the dark) (32×24, closed, hit, opened) | `survey-cache-r23-a`, `-b` | b contradicts "invisible until lit" (offered as the hint option) || **a** (CDF case) chosen; production sprites `tools/art/l06_props.py` (closed, hit, opened; the markers' `-glint` frames drawn only while lit); b moved to `concept/rejected/` |
| 9 | [Data core and airlock terminal](../../campaign/act-1-first-contact/level-06-farside/README.md#secrets-and-pickups) | **concepts**, pick a or b: a pedestal console with a teal screen and a cyan cartridge core; b wall cabinet with LED rows and an amber orb core (terminal 40×32, core 16×16, intact and released) | `data-core-r23-a`, `-b` | the screen and LEDs are darkened with the ground outside the dome light; a `-glow` frame would keep them lit || **b** (wall cabinet) chosen; production sprites `tools/art/l06_props.py` (intact, released, a `-glow` frame after the light pass; the amber orb as the `pickup-data-core` loop); a moved to `concept/rejected/` |
| 10 | [Mantis sounds](../../audio/sfx/README.md#concept-art) | **recorded** (CC0/CC-BY, `import_sfx.py`), pick a or b per sound: **telegraph** 0.6 s (a "Laser Charge Up", b "laser-charge"), **beam sweep** 1.3 s (a game-style laser beam, b death ray with crackle) | `enemy-mantis-telegraph-r23-a`, `-b`, `enemy-mantis-sweep-r23-a`, `-b` | picked by envelope and level, not listened to; sweep b is CC-BY (credits screen); the round-08 laser warning stays a possible c for the telegraph || telegraph **a**, sweep **b** (CC-BY) chosen and played by the game (`Sfx`); telegraph b and sweep a moved to `concept/rejected/` |
| 11 | [Flare sounds](../../audio/sfx/README.md#concept-art) | **recorded**, pick a or b per sound: flare **launch** (a flare-gun shot, b firework ignition), flare **burn** 3 s seamless loop (a road flare, b road flare) | `hazard-flare-launch-r23-a`, `-b`, `hazard-flare-burn-r23-a`, `-b` | launch a and burn b are CC-BY; not yet heard in the mix || launch **a** (CC-BY), burn **a** chosen and played by the game (the burn's 3 s loop back to back while a flare burns); launch b and burn b moved to `concept/rejected/` |
| 12 | [Coilwyrm regrowth sound](../../audio/sfx/README.md#concept-art) | **recorded**, pick a or b: the 0.6 s head regrowth (a wet slime, b insect growl and chitter) | `enemy-coilwyrm-regrow-r23-a`, `-b` | the fan and pops keep the existing enemy-shot and explosion sounds || **b** chosen and played at the regrowth; a moved to `concept/rejected/` (the death's bursts: [round 24](../round-24/README.md)) |
| 13 | [Perimeter beacon voice](../../audio/voice/README.md) | **audition**, pick a or b: "…Daedalus perimeter. All residents report to shelter… all residents report…" through Chatterbox and the new **public-address filter** (`pa()` in `tools/concept/audio/tts_r18.py`: horn-speaker band, a little saturation, mains hum, echoes, a short hall; `tools/concept/audio/tts_r23.py`), a: Mark F. Smith, b: Lucy Burgoyne (LibriVox, CC0 / public domain) | `voice-perimeter-beacon-r23-a`, `-b` (clips `ref-perimeter-beacon-r23-a`, `-b`) | Whisper reads the whole line back but "Daedalus"; Mark F. Smith was round 19's rejected Hammer Lead a; whether the radio cue also gets the radio filter on top is open || **a** (Mark F. Smith) cast: `refs/ref-perimeter-beacon.wav`, `uncast` removed, the line rendered through the PA filter (also the section 2 `voice_loop`); b moved to `concept/rejected/`, its clip deleted with its CREDITS.md row |
| 14 | [Level 06 in the game](../../campaign/act-1-first-contact/level-06-farside/README.md) | a game capture with `--level 6 --invulnerable` | `level-06-capture-final-r23-a` | the beam, telegraph, ore cart, survey cache and terminal are still drawn in code or as placeholders || accepted (chosen); the props in the game: `level-06-props-capture-final-r23-a` |
| 15 | [Part F numbers chosen by the agents](#part-f-numbers) | accept or change the numbers listed under [Part F numbers](#part-f-numbers) | — | the Coilwyrm's head-first time bonus has no number and is not paid || accepted as listed |
| 16 | [Level 06 briefing images](../../ui/briefing/README.md) | production art (`tools/art/briefing_images.py`, straight to production per D8): `level-06-daedalus-rim` (Okafor: the far side across the terminator, two settlements silent, Daedalus Rim's lit domes with its 2,000 people, Lancer's run into the dark by headlight along the ore rail) and `level-06-edge-scan` (Varga: the Mantis at the screen edge sweeping its beam across, side-firing guns reaching it; the Coilwyrm coming round behind the ship, its open rear, "move") | `briefing-images-final-r23-a` | the other two settlements have no names in the design ("settlement 2", "3"); the Coilwyrm is laid out at the data's 0.9 spacing, so it shows the beads-with-gaps of choice 2 (re-render after that choice); the beam wedge starts at the Mantis's centre, not its eye; the headlight cone is a faint glow || accepted (chosen); **re-rendered** (2026-10-05) for the 0.5 spacing (the overlapping chain, its path dashed beyond the tail) and the telegraph and beam from the Mantis's eye in the charged-lance look; `level-06-daedalus-rim` unchanged |

## Part F numbers

Chosen by the agents to close part F's doc gaps (choice 15); the details live in the parts'
READMEs (Decisions of 2026-10-04) and Level 06's `data.yaml`.

- **Edge warnings**: the 1.5 s of the first draft follows the code's 3 s minimum for every side and
  rear entry (easy: 4 s for Coilwyrm loop-backs).
- **Mantis**: the hard sweep interval of 2.5 s is authored, so the fire-rate lever does not apply on
  top of it; on easy the 3 s interval is divided by the lever.
- **Coilwyrm**: the members follow the head's path history at the data's spacing × the mean length
  of two neighbours (head 58 px, tail 40 px), a chain of about 330 px at the spacing of 0.5 chosen
  in choice 2 (about 480 px at the first draft's 0.9); the cut rear
  part holds still for its 0.6 s regrowth, then the new head lunges at where the ship was at
  220 px/s and leaves; the tail's first bonus pays when it goes before any other part; the chained
  death and a severed part's pops pay nothing; the head-first time bonus is left open.
- **Level 06**: 182 enemies at medium (57.7 a minute), `bounty_scale` 0.75 (typical haul 984 of
  982, perfect 1,615); the survey cache moves to t≈109.5 so the t=112 flare can light it; the
  finale's figure-8 comes at t=168; the headlight comes on at t=20 with Varga's line; a wave's
  `skip` leaves it out on a difficulty (hard's Coilwyrm pair replaces the t=146 Skitter snake).
- **Radio and music**: Rook's "six" lines move to the start of the rear strikes' warnings; the radio
  is retimed to the 1 s rule (the Varga/Okafor exchange before the finale); the base stem from
  section 1, the full stem from section 4; `ambience_from` and the beacon's `voice_loop`.

## Notes

- Level 06's unit atlas is 1 page of 2048² (57 %, the Mantis and the Coilwyrm); the shared page,
  with the glow frames, is 79 % of 2048×1024; with its backdrop the level is at 3 of 6 pages.
- Review file names: `<subject>-final-r23-a.png/.gif` (production art) and `<subject>-r23-a/b.*`
  (concepts, sounds, voices) in each part's `concept/`; their `prompts.md` entries say how they are
  made. Nothing in `assets/` is hand-edited.

## Decisions

- 2026-10-04: Opened with M4 part F.
- 2026-10-05: Closed (user): 1, 3, 4 and 5 (the Mantis, the darkness's glows and light shapes,
  Level 06's backdrop, the intel portraits) approved as **final**, the Mantis's beam moved to its
  head (beam b); 2, the Coilwyrm, accepted at spacing 0.5 (overlap, about 330 px; its death's
  bursts went to [round 24](../round-24/README.md)); 6 beam b, 7 ore cart b, 8 survey cache a,
  9 data core terminal b; 10 Mantis telegraph a and sweep b, 11 flare launch a and burn a, 12
  regrowth b; 13 the perimeter beacon is Mark F. Smith (a); 14, 15 and 16 accepted. The rejected
  variants moved to their `concept/rejected/` (CREDITS.md rows updated; the beacon's unused clip
  deleted with its row). At the close: the props' production sprites from the chosen concepts
  (`tools/art/l06_props.py`, review `l06-props-final-r23-a`, capture
  `level-06-props-capture-final-r23-a`), the chosen sounds and the beacon's voice wired into the
  game. Pending: the two Level 06 briefing images re-rendered for the 0.5 spacing and the beam from
  the Mantis's head.
- 2026-10-05: Choice 16's pending re-render done: `level-06-edge-scan` shows the Coilwyrm at
  spacing 0.5 and the Mantis's telegraph wedge and beam from its eye in the production
  charged-lance look (`tools/art/mantis_beam.py`); `level-06-daedalus-rim` re-rendered
  pixel-identical; the sheet `briefing-images-final-r23-a` rebuilt, the board unchanged.
