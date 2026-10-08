# Level 10 – Evacuation Corridor: concept and production art briefs

## loss-r32-a

Production art proposal, M5 part D (round 32; user decision D10 = a: the scripted loss's look, the
glow and the lance, at production quality with an a/b pick; a is in the game until the pick).
Variant a **"Thorn spear"**, rendered by
[tools/art/lance_l10.py](../../../../../tools/art/lance_l10.py) (`python3 tools/art/lance_l10.py`;
2D light fields at 4x supersampling, one palette per set). The sheet shows the frames and the
scene in play; the GIF plays t 115.5–121.3 at 2x over a dawn stand-in (Level 08's avenues graded to
first light, a stand-in low-air smoke layer; Level 10's backdrop was not built yet) with the
production shuttle a in its five stations' lane sway and a timeline strip (the glow at 116, the
lance sound's start at 116.8, the hit at 118, the glide's end at 121).

The look: a soft violet light swells in the cloud deck just ahead of Lifeline Three — cloud lumps
with silver pink edges occlude a violet bloom round a pink-white core, faint shafts — pulsing
quicker and brighter for 2 s; at 118 a thin violet spear with backward thorns drops from the light
onto the shuttle, the core bursts white, and a thin violet ring with eight thorn spikes runs out to
44 px; the spear breaks into glowing segments and motes (0.4 s) while the shuttle glides into
`far` as its `-wreck` frames. Brief: `a pre-rendered late-90s top-down shmup effect, an alien
violet light glowing through a thin cloud deck above a civilian shuttle, then a thin violet thorn
spear of light striking it from above with a ring flash, Vrell bio-light`.

What the renderer needs (LossLooks, D6; the data's `scripted_loss {unit: 3, t: 118, glow: 2}`),
every file additive (premultiplied on black, `GL_SRC_ALPHA, GL_ONE`):
- `cloud-glow_0..5`, 128×128, a churn loop at 10 fps from `LOSS_GLOW` (t 116), drawn **under the air
  layer** (after low-air, before the shuttles and the bullets, so it covers nothing that matters),
  centred **72 px above** Lifeline Three's current position (it follows the sway). Its strength is
  the batch alpha `intensity(τ)`, τ = t − 116: `env · (0.6 + 0.4 cos 2πφ)`, `env = 0.25 + 0.75 ·
  smoothstep(0, 1.6, τ)`, `φ = 1.25 τ + 0.625 τ²` (five pulses quickening from 1.25 to 3.75 Hz, the
  last peak exactly at the hit). From the hit it stays where it was and fades 1 → 0 over 0.8 s
  (ease out, `1 − (1 − u)²`).
- `lance_0..5`, 24×88, 4 steps a frame (0.4 s) from `SCRIPTED_LOSS` (t 118), **above the air
  layer**: its top centre (12, 0) on the glow's centre, its tip (12, 72) on the shuttle's centre at
  the hit; frame 0 is the strike (no lead), 1–2 burn, 3–5 break up. Screen-fixed: it does not follow
  the wreck.
- `lance-flash_0..7`, 96×96, 3 steps a frame (0.4 s), from the hit, centred on the hit, above the
  air layer.
- Lifeline Three switches to its `-wreck` glide at the hit (tools/art/shuttle.py; the review sinks it
  142.5 u² px, drifts it −18 u px, u over 3 s, with ship-smoke puffs from `wreck-smoke` every 6
  steps).
- The lance sound starts 1.2 s before the hit (t 116.8): its impact lands with frame 0.

Outcome (user, 2026-10-08): **rejected** in round 32 (b picked), moved to `rejected/` (the heading keeps the file's name).

## loss-r32-b

Production art proposal, as loss-r32-a: variant b **"Iris column"** (`python3 tools/art/lance_l10.py
--variant b` writes it under the same names, the step after the user picks b; until then it lives
only in its review files). Same scene, timeline and stand-in.

The look: a teal vortex opens in the cloud deck **straight above** Lifeline Three — three
logarithmic spiral arms of teal light through churning cloud turning round a violet ring that frames
the shuttle (a bullseye), with a dim violet pupil under it — growing and pulsing for 2 s; at 118 a
broad column of teal-white light lands on the shuttle (seen from above: a disc with a teal-white
wall and violet rim, streaks converging into it), narrows from 30 to 14 px, collapses to a white
point and rings out in three rippling shock rings (teal leading, violet trailing) to 60 px with
teal motes (0.6 s). Brief: `a pre-rendered late-90s top-down shmup effect, a teal alien vortex
opening in the clouds above a civilian shuttle, then a broad column of teal-white light lands on it
and collapses into rippling teal and violet shock rings, Vrell bio-light`.

What the renderer needs, as a with these differences:
- `cloud-glow_0..5`, 128×128, 10 fps; the six frames turn the vortex a third of a turn (the arms'
  period), so the loop is seamless. Centred **on** Lifeline Three (no offset), under the air layer;
  the same `intensity(τ)` as batch alpha, and **scaled** 0.55 → 1.0 with smoothstep over the 2 s
  (an unlit additive effect may be scaled), kept at 1.0 through the fade.
- `lance_0..11`, 128×128, 3 steps a frame (0.6 s) from the hit, centred on the hit, above the air
  layer: 0–2 the column (frame 0 at the hit, no lead), 3 the white point, 4–11 the shock rings.
  There is **no** `lance-flash` in b (the rings are in the lance frames).

Weak spots (both): on the stand-in, not on Level 10's real section-3 backdrop (the viaduct and the
creep districts), whose colours decide how strongly the violet reads; the glow under the air layer
is lit over by nothing but the ground and the smoke, so it reads as light on the clouds below rather
than in a deck above (the layer the design names, `deep`, is hidden by the ground in section 3);
a's glow sits 72 px up the screen, in the gap between Lifeline One and Three; b's iris arms are
blocky at 40 colours and its column hides most of the shuttle for 0.2 s.

Outcome (user, 2026-10-08): **chosen** in round 32. `python3 tools/art/lance_l10.py` (its `PRODUCTION` is now b) writes it under the game's names; a's `lance-flash` frames are gone from `assets/sprites/`, so the game draws the column.

## backdrop-final-r32-a

Production art, M5 part D (round 32; user decisions D10 = a: straight to production, D11 = a:
first light). Not a mockup: the sheet and GIF of Level 10's backdrop in `assets/backdrop/level-10/`
and the ferry hatch's sprites (`ferry-hatch_0..2`, `ferry-hatch-break_0..7`), rendered by
[tools/art/backdrop_l10.py](../../../../../tools/art/backdrop_l10.py) (`python3 tools/art/backdrop_l10.py`,
`--props` for the hatch, `--proposal` for the backdrop block), Level 08's production kit
(`tools/art/backdrop_l08.py`, imported as a library) with Level 09's creep helpers
(`tools/art/backdrop_l09.py`). The look: the chosen megacity scene
([parallax-r03-b](../../../../art-direction/concept/parallax-r03-b.png)) at first light: the kit's navy
city lifted toward a cool blue-grey in the west and a warm peach in the east, a little more with
every section; the water of the coast and the open lagoon translucent over the `morning-sky` deep
layer (blue-grey in the west, gold in the east, clouds lit pink). Section 1 Eko spaceport: the
apron in concrete slabs, amber taxiway lines with blue edge lights, hangars, the terminal blocks,
flood masts, service vehicles, the `launch-pads` piece with the five pads round the shuttles' t=1
points (the README's liftoff pads), hazard-striped edges, scorched blast centres, numbered in bars.
Section 2 the suburbs: house roofs, parks and football pitches, the motorway up the middle jammed
bumper to bumper (red tail lights), refugee columns as lights on its shoulders, CDF gunships (Level
08's gunship at dawn) flying south, the other way. Section 3 the maglev viaduct (deck, guideways,
the pylons' shadows, a stalled train) between two creep districts: teal-black ground creep, violet
chitin on roofs and the towers' upper floors, burnt blocks with fires and smoke plumes, towers in
true perspective (their walls drawn the way the game draws them since part C). Section 4 the coast:
a fishing village, the coast road with stalled cars and refugee lights, the sea wall and beach,
wooden piers, sandbars and shallows, fish-trap stakes, pirogues, the capsized ferry on its sandbar
(faded red antifouling, rust streaks, keel, bilge keels, propellers; a patched plate where the hatch
sits). Section 5 the open lagoon: shallows over sand, shoals, stakes, red channel buoys blinking.
Low-air haze (light) and smoke and sea mist (medium) drift west on the sea wind; thin dawn mist and
smoke wisps on high-air. A cross highway hides every section seam (the last two as causeways over
the water). The hatch (36×32, a ground target of 50 HP with the hidden crate (8 when the sheet was made)): an oval
watertight hatch on a plate of the hull, its rim, door, hand wheel, six dogs and an amber emergency
lamp; hit: dented and torn; open: the door blown off and bent aside, the crate in the dark hold; the
break-apart in six pieces as Level 09's cocoon. The sheet shows the tiles, pieces, towers, walls and
the hatch's frames, and 18 composites as the game draws them with the five production shuttles
(variant a) at their pads, easing to their stations and swaying, and the hatch on the ferry; the GIF
plays the liftoff, t 0–8 at 10 fps. Brief: `a pre-rendered late-90s top-down shmup backdrop, a West
African megacity's outskirts at first light: a spaceport, jammed suburbs, a maglev viaduct between
alien-infested districts, a lagoon coast with a capsized ferry, the open lagoon; navy city with
warm dawn light from the east, posterized, no people`.

## level-10-capture-final-r32-a

Round 32, a capture of the game, not generated art (`.png` sheet, `.mp4` with the game's sound),
retaken on 2026-10-08 **after the fix pass** (the Wraith's fixed fan, the popcorn routed round the
band, the 3 s wreck glide, the 50 HP hatch, the raised rear banner, the mint home bar) from the
uncommitted working tree, the atlases packed from the current `assets/` by `:desktop:installDist`
(an isolated build directory). `terran-vanguard --bench 228 --settings <file> --level 10
--invulnerable --escort rook:autocannon:2 --loadout
front=pulse-cannon:3,left=bomb-rack,right=autocannon-pod,rear=tail-gun:1 --special airstrike:2` at
medium (the balance plan's L10 fit: the Tail Gun bought, Rook's Autocannon) on a private Xvfb
display (960×540; settings: a 960×540 window at 0,0, `controls.auto-fire=true`). Video: `ffmpeg -f
x11grab -draw_mouse 0 -framerate 30 -copyts`; sound: OpenAL Soft's `wave` driver writing the game's
mix to a file, nothing played aloud, aligned by the file's end (it starts 4.64 s before t = 0; the
lance's sound rises from t 116.8 and peaks at the strike); level time t = 0 at the game's `[display]
window` log line. The ship is flown by XTest key events from a small Python driver (dead reckoning
at 270 px/s): from t 8 a slow sweep along its start line (132–348 px, a 4 s cycle), both Airstrike
charges at t 63.8 and 90.0, a wall reset and x = 322 for the ferry hatch from t 146, **up 120 px at
t 153.0 to meet the crate and back down at t 156.8**, back to the sweep at t 165. Two runs: run 1
without the move up (otherwise the same keys) shot the hatch open at t ≈ 150.8 about 150 px below
the top edge, and its crate, drifting down at about 40 px/s, expired at y ≈ 370 (t ≈ 156.7) above
the ship on its start line (y ≈ 444): 199 / 318 kills, secret 1 / 1 (0 credits), 1 / 4 home (+40),
A, 1,768 credits. Run 2 is the one shown: the liftoff t 1–7, Lifeline Three's scripted loss t 118,
Lifeline Five lost under the finale's fans at t 194.6, One, Two and Four home at the climb-out
(t 196–198); the hatch open at t ≈ 150.8, its crate caught at t 154.3 (+160); 207 / 318 kills,
secret 1 / 1 (160 credits), 3 / 4 home (+120), A (rating 80), 2,057 credits. The video is the whole
of run 2 from t −1 to the debrief (228 s, H.264 CRF 25, AAC 128 kb/s, the sound 3 dB down so the
encode does not clip). The sheet: fifteen frames with the left HUD panel: the shuttles on the pads
(t 1.0) and climbing (4.0), a cloaked Wraith over the band (51.0), its decloak flash (55.05), the
fixed 40° fan going straight up through the band (57.2) and one of its shots hitting the lead
shuttle (57.6), the lance (118.2), a Mote Swarm's loop-back up the left edge, clear of the band
(131.0), the hatch shot open with its crate rising (151.0), the crate reaching the ship (154.3), the
rear-warning banner above the band, clear of the shuttles (158.0), Lifeline Five lost in the finale
(194.7), the climb-out (196.4), the three home shuttles' pale mint bars (200.0) and the debrief.

## loss-capture-final-r32-a

Round 32's loss look a as built in the game, a capture, not generated art (`.png` sheet, `.mp4` with
the game's sound), retaken on 2026-10-08 after the fix pass from run 2 of
[level-10-capture-final-r32-a](#level-10-capture-final-r32-a): the glow (`cloud-glow`, under the air
layer, 72 px above Lifeline Three, following its sway), the lance (`lance` + `lance-flash`), the
shuttle's `-wreck` glide (darker, smoking, sinking and sliding outward over 3 s) and the lance's
sound (from t 116.8, the strike at 118.0) with the music's duck. The `.mp4` is t 110–122 (12 s), the
whole screen with the HUD, the sound 3 dB down (H.264 CRF 20, AAC 160 kb/s). The sheet: eleven
play-field frames at 1×, t 115.8 (before), 116.5 (the glow starts), 117.2, 117.8 (its peak), 118.1
(the lance), 118.3 (the flash ring), 118.8 (the wreck, darker and smoking), 119.6 and 120.4 (the
wreck sinking and sliding right of its station), 121.0 (a small dark shape, fading) and 121.4 (gone:
it shows for about 3 s, t 118.3–121.2).

## level-10-readability-r32-a

Round 32, a capture of the game, not generated art: 128 px crops of run 2 of
[level-10-capture-final-r32-a](#level-10-capture-final-r32-a) (retaken on 2026-10-08 after the fix
pass) at 1:1 and at 2× (nearest neighbour) at the game's 960×540: a cloaked Wraith's shimmer over a
shuttle (t 51.0), the decloak flash (t 55.05), the fixed fan going straight up through the band
(t 57.2) and a shot of it hitting the lead shuttle (t 57.6), Lifeline Three's dark wreck sliding out
with its smoke (t 119.6), a Mote Swarm looping back up the left edge (t 131.0), the ferry hatch shot
open with its crate rising (t 151.0) and the crate reaching the ship (t 154.3), the rear-warning
banner above the band, clear of the shuttle below it (t 158.0), and a 196×56 px crop of the HUD's
shuttle tracker with the three home shuttles' pale mint bars beside the two lost ones' dark bars
(t 200.0).
