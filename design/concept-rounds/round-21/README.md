---
title: Concept round 21 — M4 part E, Level 05
design: approved
implementation: n/a
art: chosen
depends-on: [../../campaign/act-1-first-contact/level-05-crater-nest]
updated: 2026-10-04
---

# Concept round 21 — M4 part E, Level 05

## Summary

The production-art round of M4 part E ([roadmap](../../tech/roadmap/README.md#m4-parts)): Level
05's Gorgon Frigate, Polyp Mortar (with its death) and mass-driver sled, rendered as **final** art by
the generators in [tools/art/](../../../tools/art/README.md) into `assets/`; the level's two briefing
images; a/b concepts for the small props and sounds that were placeholders (user decision D8); the
Driver Control voice audition (user decision D4); a game capture of the level; and the numbers the
agents chose to close part E. Open [index.html](index.html) in a browser (regenerate with
`python3 tools/concept/board.py 21`). Every art choice is *approve as final* or *redo* (say what to
change); the concepts, sounds and voices are *pick a or b*; only an approved part gets `art: final`.
Level 05 is playable with `./gradlew :desktop:run --args="--level 5"`.

## Choices

| # | Part | What is proposed | Review files | Weak spots (Claude) | Outcome |
|---|---|---|---|---|---|
| 1 | [Gorgon Frigate](../../enemies/bosses/gorgon-frigate/README.md) | production art of the chosen r06 a model (`tools/art/gorgon_frigate.py`): the bell in 4 crown stages, head, stump and 5 neck pieces at 15 headings, the death's petals and ichor, the core glow | `gorgon-frigate-final-r21-a` (png, gif) | at its hover y 110 the tendrils are cut off at the top edge of the field; the necks have only 15 headings (±78.75°) || redo: see item 11 (the death; the sprites themselves are right in the game, user 2026-10-04); approved — final with 11 |
| 2 | [Polyp Mortar](../../enemies/ground/polyp-mortar/README.md) | production art of the chosen r04 a model (`tools/art/l05_hazards.py`): 8-frame idle pulse at 44×44, the acid blob, the impact marker | `polyp-mortar-final-r21-a` (png, gif) | no firing frame (the lob is the shot) || approved — final |
| 3 | [Polyp Mortar death](../../enemies/ground/polyp-mortar/README.md) | production art (`tools/art/l05_hazards.py`): `polyp-mortar-death` (lime acid spray, additive) and `polyp-mortar-tatters` (tentacles and shell shards, solid), 12 frames each with the small burst | `polyp-mortar-death-final-r21-a` (png, gif) | the tentacles read as thin pale spikes; not yet seen in the game || approved — final |
| 4 | [Mass-driver sled](../../campaign/act-1-first-contact/level-05-crater-nest/README.md#hazards) | production art (`tools/art/l05_hazards.py`): the lit sled (24×48), its additive motion streak and the lit rail lamp over Level 04's lamp frames | `sled-final-r21-a` (png, gif) | the streak is only a faint band at the top while the sled is on screen for 0.4 s || approved — final |
| 5 | [Level 05 props](../../campaign/act-1-first-contact/level-05-crater-nest/README.md#concept-art) | **concepts**, pick a or b per prop (`tools/concept/props_r21.py`): low-gravity **rocks** (16×16: a plain regolith, b scorched basalt with a lime crust), the stuck sled's **ore canister** (40×28: a hazard-banded cylinder in a clamp yoke, b open ore skip with clamp jaws), the **acid splash** decal (40×40: a wet lime splatter, b etched burn with a teal pool). The **battery outline** gets no concept: the game's 1 px objective-colour outline stays (agent's call; say if you want variants) | `rocks-r21-a`, `-b`, `ore-canister-r21-a`, `-b`, `acid-splash-r21-a`, `-b` | the rocks are small at 1x; a's grey rocks are close to the ground's greys; the canister's beacon change (dark/lit) is subtle; the splash is drawn as the mortar's death decal (its spec), not at each lob's impact || rocks a, ore canister a, acid splash b chosen (the others moved to `concept/rejected/`); production sprites `tools/art/l05_props.py`, review `l05-props-final-r21-c`, accepted at the close |
| 6 | [Level 05 briefing images](../../ui/briefing/README.md) | production art (`tools/art/briefing_images.py`): `level-05-crater-nest` (Okafor: the crater beside the mass-driver line, batteries A–D, Lancer's run over the rim, the sleds) and `level-05-mortar-scan` (Varga: the mortar's lob to its marker a second ahead, the ship moving out, an unknown contact in orbit) | `briefing-images-final-r21-a` | the orbit contact is a plain red square so the frigate stays a surprise; the crater map is schematic, not the real battery layout || approved — final |
| 7 | [Sled and mortar sounds](../../audio/sfx/README.md#concept-art) | **recorded** (CC0/CC-BY, `import_sfx.py`), pick a or b per sound: sled **whine** while the lights chase (a charge-hum loop, b rising charge), sled **pass** (a rail-gun crack, b rushing flyby), mortar **lob** (a real mortar thump, b organic spit), mortar **impact** (a wet splat, b acid sizzle) | `hazard-sled-whine-r21-a`, `-b`, `hazard-sled-pass-r21-a`, `-b`, `enemy-mortar-lob-r21-a`, `-b`, `enemy-mortar-impact-r21-a`, `-b` | the sled pass a is by a since-deleted Freesound account (CC0, its own mix); whine b is CC-BY (credits screen); not yet heard in the mix || mortar impact a, mortar lob a, sled pass b, sled whine a chosen and played by the game (`Sfx`); the others moved to `concept/rejected/` |
| 8 | [Driver Control voice](../../audio/voice/README.md) | **audition**, pick a or b: the t=12.5 warning ("Mass driver's still on automatic, Aegis. Sleds every five seconds. Watch the rail lights.") through Chatterbox and filter b (`tools/concept/audio/tts_r21.py`), a: Alex Foster, b: Rebecca (LibriVox, public domain) | `voice-driver-control-r21-a`, `-b` (clips `ref-driver-control-r21-a`, `-b`) | both clips are low (81 and 113 Hz); Whisper hears "Aegis" as "ages", as in round 19 || a chosen: `refs/ref-driver-control.wav`, `uncast` removed, its three lines rendered; b moved to `concept/rejected/`, its clip deleted with its CREDITS.md row |
| 9 | [Level 05 in the game](../../campaign/act-1-first-contact/level-05-crater-nest/README.md) | a game capture with `--level 5 --invulnerable` | `level-05-capture-final-r21-a` | placeholder props and Level 04's backdrop; the frigate's tendrils cut off at the top edge || redo: see item 12 (it was taken before the frigate's production sprites were wired in) |
| 10 | [Part E numbers chosen by the agents](#part-e-numbers) | accept or change the numbers listed under [Part E numbers](#part-e-numbers) | — | the batteries need the balance plan's Level 05 fit; the starter fit cannot clear them in time || accepted as listed |
| 11 | [Gorgon Frigate death](../../enemies/bosses/gorgon-frigate/README.md) | **redo of 1's death** ("more explosions and pieces; now it just disappears"), the Leviathan's way (`tools/art/gorgon_frigate_death.py`, the game's `SetPieceDeath`): the bell broken into five wedges along jagged seams (charred, the torn flesh glowing) and the three necks with their stumps falling away, each with 3 tumble frames rendered under the fixed light; 13 large blasts over the bell and necks from step 24 peaking at the swap (step 54, 0.9 s), where the petals tear off; then the chunks drift apart, sink, darken and fade for 1.8 s under 8 trailing blasts and ichor; extra explosion sounds layered at the swap | `gorgon-frigate-death-final-r21-b` (png, gif) | the cut faces show as dark char with violet veins when a wedge tilts; the necks tumble as pre-rendered heading frames, so they turn in 11.25° steps; a neck still bent at the death straightens at the swap | approved — final (the frigate's art final) |
| 12 | [Level 05 in the game](../../campaign/act-1-first-contact/level-05-crater-nest/README.md) | **redo of 9**: a new capture with the production frigate (`--level 5 --invulnerable --loadout front=pulse-cannon:4`) | `level-05-capture-final-r21-b` | the bell stays cut off at the top edge (hover y 110); the batteries show FAILED (`--invulnerable` keeps the level going); the props are still the placeholders; a sled is small at the bottom of its frame | accepted (chosen) |

## Part E numbers

Chosen by the agents to close part E's doc gaps (choice 10); the details live in the parts' READMEs
(Decisions of 2026-10-03/04) and Level 05's `data.yaml`.

- **Arena**: section 3's heavy peak is a `peak` (115–122 s), so the arena stays section 5; the
  music sting crossfades in over 0.5 s.
- **Batteries**: four ground-target groups `Battery A`–`D`, tracker `BATTERIES A B C D`; a battery's
  four units enter 0.5 s apart; the failure line names the battery through `{group}`.
- **Sleds**: on Level 04's rail at x 432, ground layer; the first at t = 16.5 s, after Driver
  Control's warning; the stuck sled's found line goes to Rook.
- **Polyp Mortar**: marker 1.0 s, impact circle 32 px (a diameter), ring 8 (12 on hard) starting
  12 px outside it; first lob 1.5 s after entering; the blob is not shootable and keeps flying when
  the mortar dies; the marker stays where the ship was, in the play plane.
- **Gorgon Frigate**: phases end on the head count (phase 1 ≈ 71 %); invulnerable in the descent
  (60 px/s), settles with its bell at y 110, sways ±40 px over 6 s; bell hit box 200×120, heads
  36×36 at rest 170–190 px below the bell's centre, core 60×60, neck segments 22×22 with 0.12 s lag
  and a 30° bend (55° enraged); stream interval 0.5 s; spiral 3 arms, a bullet per arm every 0.25 s,
  90 px/s, 60°/s; hard core ring 16; Skitter streams at the settle and every 10 s in phase 1; a 1 s
  crown opening.
- **Pacing**: 14 small Skitter streams (+14.5 % credits); the t=62 stream keeps its eight on easy.
- **Tests**: Level05Test and PacingTest fly the balance plan's Level 05 fit (Pulse Cannon L2, two
  Autocannon Pods, the second shield; Pulse Cannon L3 on hard).

## Notes

- The frigate's tendrils are cut off at the top edge of the field at its hover y 110 (choices 1
  and 9): raise the hover point, or accept it as the "too big for the screen" look.
- The shared sprite atlas pages are now **96.7 %** and **91.8 %** full (91.5 % before the props'
  production sprites): a risk for parts F–G, which will need a third page or a split atlas.
- Review file names: `<subject>-final-r21-a.png/.gif` (production art) and `<subject>-r21-a/b.*`
  (concepts, sounds, voices) in each part's `concept/`; their `prompts.md` entries say how they are
  made. Nothing in `assets/` is hand-edited.

## Decisions

- 2026-10-04: Opened with M4 part E.
- 2026-10-04: User verdict on items 1–10: 2, 3, 4 and 6 (the Polyp Mortar, its death, the sled,
  Level 05's briefing images) approved as final; 5, rocks a, ore canister a and acid splash b
  chosen; 7, mortar impact a, mortar lob a, sled pass b and sled whine a chosen (wired into the
  game); 8, Driver Control's voice a cast; 10, the part E numbers, accepted as listed; the
  rejected variants moved to their `concept/rejected/`. 1, the frigate: its death "needs more
  explosions and pieces; now it just disappears", redone as item 11 (the sprites are right in the
  game); 9, the capture, showed the placeholder shapes (taken before the production sprites were
  wired in), redone as item 12. The round stays open for items 11 and 12.
- 2026-10-04: Closed (user): items 11 (the Gorgon Frigate's death redo) and 12 (the new capture)
  accepted, so the frigate's art is final. The props' production sprites from the chosen concepts
  (rocks a, ore canister a, acid splash b; `tools/art/l05_props.py`, review
  `l05-props-final-r21-c`) were wired in at the close, the concepts already being accepted.
  Level 05 stays `art: chosen`: its backdrop still reuses Level 04's images.
