# Level 14 – concept and production notes

Round 36 (M5 part H step H2; decision H8 = a: a/b for the root burst and the daylight, one version for
the props and the outro images; the user is on vacation, so all of it is pending the user's review).
None of it is prompted: every file is rendered by a generator in `tools/art/`.

## root-burst-r36-a

The root burst, look a "breaking paving", rendered by
[tools/art/root_burst.py](../../../../../tools/art/root_burst.py) (`python3 tools/art/root_burst.py a`;
nothing goes to `assets/`: the backdrop and props step writes the chosen look's frames). Brief (the
README's *Hazards*, H2 = a): a point on the ground 100–400 px below the top edge that scrolls with the
ground; a 1.0 s telegraph, the eruption (0.5 s), then a Spine Turret and a Polyp Mortar grow in at its
two growth points over 1.5 s (hard 1.0 s), invulnerable and silent, and fire. Look a: the paving cracks
outward from the point and the two growth points (dark jagged cracks with lit edges, quick at first so
the telegraph reads, slabs lifting, a violet glow seeping up through the cracks in the last 0.4 s), dust
puffing up out of the cracks and rising (4 translucency steps), then a slate root of the Siege Spire's
tissue arches out of the street with two dark knots, throwing up paving chunks in a ring of dust; the
growths swell out of the knots (the unit's own frame scaled 0.35 → 1 and tinted from the creep's dark
violet). The root's last frame stays as the crater; the t=72 Ravager crater is a burst without growths.
The sheet: a time strip of one burst, the decal (8 frames, 112 px), the puffs (12 frames, 128 px), the
eruption (6 frames, 128×96), the grow-in; the GIF: two bursts scrolling at 140 px/s on a stand-in old
town (the concept city ground), the first shots at 2.5 s.

## root-burst-r36-b

The root burst, look b "creep surge", same generator (`python3 tools/art/root_burst.py b`): violet creep
tissue surges in from the gutters on both sides (tapering tendrils with glowing veins converging on the
point and the growth points, pooling there), a spore mist rising off it (fewer, slower, violet-grey
puffs with glowing motes), then a sac mound swells up and two buds split their five petals open; the
growths swell out of the buds (the same grow-in as a).

## daylight-r36-a

The daylight section after the Spire's death, look a "golden hour", rendered by
[tools/art/daylight_l14.py](../../../../../tools/art/daylight_l14.py) (`python3 tools/art/daylight_l14.py a`;
functions for the backdrop step, nothing goes to `assets/`). Brief (section 6 and *Act end*): 12 s at
140 px/s with no enemies and no music; on a real-seconds timeline from the death the canopy withers (its
teal veins die to grey-brown, 0–2 s), tears open cell by cell with the torn edges curling back (1–7 s)
and fades (to 10 s), and the light comes up on the old town and the lake. Look a: low warm sunlight from
the west (over the lake, the screen's left) breaking through the tears in long diagonal beams (additive);
the city graded warm and bright, the creep's teal dying to brown, the long shadows kept. The ground is
the chosen Geneva scene's city texture (`tools/concept/scenes_r10.py`); the still is 8 s after the death,
the strip 0, 3, 6, 9 and 12 s.

## daylight-r36-b

The daylight section, look b "pale dawn", same generator (`python3 tools/art/daylight_l14.py b`): a cool,
even morning light; a haze over the city (4 translucency steps, ≤ 40 %) that burns off from the lake
inward over the section; no beams; the city graded clear and cool with lower contrast.

## backdrop-final-r36-a

Production art, M5 part H (round 36; H8 = a, straight to production from the chosen
`scene-geneva-r10-a`, pending the user's review). Not a mockup: the sheet and GIF of Level 14's backdrop
in `assets/backdrop/level-14/`, rendered by [tools/art/backdrop_l14.py](../../../../../tools/art/backdrop_l14.py)
(`python3 tools/art/backdrop_l14.py`: the proposal, every image, the checks and this review; `--review`
only the review; `--proposal` only [backdrop-proposal.yaml](backdrop-proposal.yaml), the level's block for
the main agent to merge). The city is the concept's method (`tools/concept/scenes_r10.py`'s helpers,
imported unchanged) rebuilt from plans: continuous street gaps with sidewalks, perimeter blocks of row
houses round courtyards with two-tone ridged roofs lit from the top left (tile in the old town, slate and
zinc in the 19th-century quarters, a few verdigris), party walls, chimneys and skylights, cast shadows,
parks and tree rows, parked cars, sodium lamps with their pools, the canopy's mottled shadow baked into
the ground (high-air is additive in the game and cannot darken). Every tile wraps in the middle of a wide
cross street, so its seam is plain asphalt. Sections: 1 the lakefront (Lac Léman on the left, the quays,
the promenade), 2 the old town (creep veins in the gutters), 3 the root field (three roots as thick as
trams per tile, lit as tubes with teal seams, cracked paving, collapsed houses, creep over the roofs),
6 the daylight (the old town in look a's golden hour, lamps out, the creep withered). Sections 4-6 are one
unique strip of four pieces (the arena halts, so its own tile set is never drawn and the halt's top 69 px
would show the next one): the approach's avenues converging on the Concord roundabout, the roots of the
root field coming up to the plaza, the plaza (broken ring of UTC flags, creep mound) under the boss's
anchor (x 240, 196 px below the top at the halt) with six avenues along its roots, and the city north of
it graded from dusk to daylight by its distance past the arena frame's top. Set pieces cut from the tile
under them (the tile's own render with the piece added, cut where it differs, so their edges fall on
identical pixels): the Rhone with the Pont du Mont-Blanc under the armoured car, Saint-Pierre's square
under the turret nest (the church's nave, transept and apse; its two west towers are perspective tower
pieces with a limestone wall texture and verdigris pyramids), the Place du Molard under the hive nodes
with creep round them. Also: the Jet d'Eau's leaning plume (the concept's particle streams, static),
the Ravager pack's crater (root burst a's last frames), tree-lined boulevards with tram rails over every
seam between tile sets, Hammer flight (three CDF bombers, the Airstrike's model rendered at both headings)
passing below on low-air, a few moving cars; the spore haze banks (violet lit from below, spores) and the
canopy's teal veins (additive, period 540). The game's daylight looks beside the block (step H6 draws
them on the real clock from the Spire's death): `canopy-tear_0..3` (daylight_l14's withering, tearing
membrane at 1.5, 3.5, 5.5 and 7.5 s, straight alpha) and `daylight-beams_0..1` (look a's beams at 5 and 7.5 s,
additive; four and two frames: Level 14 stands at 6 of its 6 atlas pages).
The sheet: the tile sets, banks, canopy, the strip at 1/3, the pieces, the tear frames, composites as the
game draws them with stand-ins for the units and the Spire (production sprites), and after a kill the
daylight; the GIF: the ease into the halt, the halt held, the daylight after a kill.

## l14-props-final-r36-a

Production art, one version (no concept: designed to the README, the chosen Geneva scene's light and the
secret targets' rule 7), rendered by [tools/art/l14_props.py](../../../../../tools/art/l14_props.py)
(`python3 tools/art/l14_props.py`; the frames go to `assets/` only with `--assets`, in the content step's
time). `armoured-car_0..7` (40×60, nose up, parked 12° askew): a six-wheeled CDF armoured car (slate-grey
hull with the CDF-blue band, a small turret with a short cannon, vision blocks, stowage boxes, a rear
cargo hatch) under violet creep tissue with glowing veins; 0 fully under creep, 1–5 one blob fewer per hit
(a scorch where it burnt off), 6 cleared, 7 the hatch sprung open with the amber CDF supply crate
inside; the 1 px light rim of rule 7. The review places it at the Rhône's mouth by the Pont du
Mont-Blanc in the Geneva city texture. The level's other props reuse existing art (the Hammer bombers,
Level 08's traffic; the drone's armour patch enters without a drone sprite).

## act-2-outro-final-r36-a

Production art, M5 part H step H2 (round 36; the user is on vacation, pending the user's review). It is
not a mockup: it is the sheet of the Act 2 outro's four briefing images, rendered by
[tools/art/outro_l14_images.py](../../../../../tools/art/outro_l14_images.py)
(`python3 tools/art/outro_l14_images.py`, which writes only this sheet; `--assets` writes
`assets/ui/briefing/act-2-outro-<name>.png` once the data claims them). Commander Okafor speaks every
page. Brief, as briefing-images-final-r13-a: `the tactical display of a late-90s space-combat briefing,
672x240: a dark navy screen with a fine cyan grid and scan rows, holographic planets, dashed cyan
routes, red and violet hostile markers, amber objectives, small pixel labels; one map or mission image
per briefing page`. Per image:

- **act-2-outro-stump** (p1, grim): Geneva Concord at sunrise from above as a recon photo in the display.
  It is the chosen round-10 city (`tools/concept/scenes_r10.py`'s city texture, without the Vrell roots
  and the rotunda) graded warm toward a sun low over Lac Léman, the lake bright gold with glitter. The
  Siege Spire's withered stump (since step H5 the production `siege-spire-stump` over its six dead
  roots, `siege-spire-root_1..11`, placed by `assets/pivots/siege-spire.json` as the game draws them after
  the lift, warmed by the low sun; until then the concept model `render/boss_models.spire(stump=True)`)
  stands on the Concord plaza where the trunk stood, its long shadow thrown away from the sun. Torn, withering shreds of the violet canopy hang at the top right. The tally: Vrell
  forces on Earth, coordination lost within the hour, some digging in, most scattering; Nova Lagos,
  Geneva Concord and the rest cleared city by city.
- **act-2-outro-names** (p2, grim): a Nova Lagos street (Ikoyi) at dawn, front on. A long memorial wall of
  eight panels, "WE REMEMBER" at its top, rows of engraved names with photos and paper notes pinned
  between them. Behind it a damaged skyline under a cold grey first light. Hundreds of candles and a few
  flowers along its foot, a warm band of light. A few people stand dark against it with their heads
  bowed, one kneeling to light a candle. Quiet and dignified, no gore. The counts still running: Nova
  Lagos, Geneva Concord, the CDF.
- **act-2-outro-mars** (p3, grim): the long-range plot. Mars is a holographic planet at the right edge in
  a rust ramp (dusky regions, the Valles Marineris scar, the south polar cap). Ares Landing has a pulsing
  ring of red and amber, under siege; Tharsis Foundry is cut off. Vrell chevrons and violet contacts hold
  orbit. The priority signal runs as a dashed amber track to a small Earth at the left, "received 1 h
  ago". The message box reads PRIORITY ONE, Ares Landing, Mars: under siege, the colonies holding alone,
  request all available, with its repeating pulse trace, and "HOLDING 21 DAYS" in large amber.
- **act-2-outro-aegis** (p4, fierce): the CDF coastal airbase's apron at dusk from above, the floodlights
  coming on and the last light from the west. Lancer's Stormhawk, Rook's Ember and Aegis Two to Four sit
  on their pads (production sprites) with long shadows. Behind them is the heavy transport bound for
  Mars: a long white fuselage with a raised cargo spine and a CDF-blue stripe, a broad delta on two
  engine nacelles, canards, twin fins, rows of lit windows, nav lights and the ramp down at its tail. It
  was built for this image from the render package's primitives with the evacuation shuttle's
  materials. Labels: "SHIPS OUT T-72 H", "EARTH TO MARS", "WE TAKE THE FIGHT TO THEM", "AEGIS ACTUAL OUT".

## level-14-capture-final-r36-a

Round 36, a capture of the game, not generated art (`.png` sheet, `.mp4` with the game's sound), taken on
2026-10-12 (step H7) from a snapshot of the uncommitted working tree (07:13), built by `:desktop:installDist`
into an isolated build directory. `terran-vanguard --bench 480 --settings <file> --level 14 --difficulty medium
--loadout front=lance-laser:5,left=micro-missile-pod:5,right=micro-missile-pod:5,rear=tail-gun:2
--special airstrike:3 --invulnerable` (Rook flies by default, his Autocannon) on a private Xvfb display
(960×540; settings: a 960×540 window at 0,0, `controls.auto-fire=true`). **With `--invulnerable`**: two runs
without it (the same fit, and pulse-cannon 5 with pods 3) lost the ship 26–35 s into the fight, again after
every retry from the boss checkpoint: `--loadout` cannot fit cores, so the debug start flies the starting
armour 60 and shield Mk I 20 (the plan's L14 ship has 100 and Mk II 30), and the driver only dead-reckons.
Video: `ffmpeg -f x11grab -draw_mouse 0 -framerate 30 -copyts`; sound: OpenAL Soft's `wave` driver
(`ALSOFT_DRIVERS=wave`) writing the game's mix to a file, nothing played aloud, aligned at the game's
`device reopen` log line. **t** is seconds since the game's `[display] window` log line. The ship is flown
by XTest key events from a small Python driver (dead reckoning at 270 px/s): sweeps at the bottom, under
the armoured car (x 330, t 13–19.5), more than 60 px beside each root burst at its telegraph and under it
1.2 s later, then in the arena parked 2.5 s under each part's column in turn (y 90); Airstrikes at t 76,
114 and 150. A screen probe (the left HUD panel's colour) finds the debrief and presses Enter on a fixed
schedule through the debrief, the act summary and the four outro pages.

**115 / 117 kills (98 %)**, ROOTS 8 / 8 (+ 101 credits), **secret 1 / 1** (its crate missed: the SECRETS
credit row reads 0), the Siege Spire's bar from t 108 to its kill at t ≈ 194.5 (**1:26**, par 2:10), A+,
2,965 credits. Phases: roots to t ≈ 150.5, the maw to t ≈ 162.3, the lift and the free spire to the kill;
the death chain to t ≈ 197.6 (a full-screen white flash), the daylight to t ≈ 214, the debrief at 218.
The video is the whole run from t −1 to 316, through the act end into the hangar (H.264 CRF 29, AAC
128 kb/s, the sound 3 dB down, 21.2 MB). The sheet: nineteen frames with the left HUD panel; the two
briefing pages and the hangar come from a second run (`--level 13 --invulnerable`, not firing, won, then
Enter through its debrief into Level 14's briefing), the phase-1 frame at t 126 from a run without
`--invulnerable` (the hull low, Okafor's warning).

## level-14-capture-fixes-r36-a

Round 36, a supplementary capture of the game (`.png` sheet, `.mp4` with the game's sound), taken on
2026-10-12 (step H8) of the step H7b fixes, from a snapshot of the uncommitted working tree (09:02) built by
`:desktop:installDist` into an isolated build directory. The recipe of
[level-14-capture-final-r36-a](#level-14-capture-final-r36-a): the same fit and command line
(`--invulnerable`), Xvfb, x11grab, the `wave` driver and driver (Airstrikes at t 76, 114 and 150), here with
`--bench 250` and no menu presses. **115 / 117 kills, ROOTS 8 / 8 (done at t ≈ 93.5), secret 1 / 1, the
Spire's bar from t 108.3 to its kill at t ≈ 192.9 (1:24, par 2:10), A+**. The video (H.264 CRF 29, AAC
128 kb/s, the sound 3 dB down, 1:53, 11.1 MB) joins four cuts with the level time and a caption burnt in
(`drawtext`): t 12–21 (Hammer flight, the car), 61–98 (the four root bursts), 99–113 (the Choir, Varga,
the boss arrival) and 164–217 (the lift to the end of the daylight). Times of the sounds are from
cross-correlating the voice files and the withering template with the mix. The sheet (75 %): twelve
frames of that run and, in the bottom row, three of a second run **without `--invulnerable`**: the balance
plan's L14 fit,
`--loadout front=pulse-cannon:4,left=micro-missile-pod:2,right=micro-missile-pod:2,rear=tail-gun:2,generator=1,shield=1,armor=2 --special airstrike:4 --escort rook:autocannon:1`, the same
driver dodging at random in the arena (y 60), Airstrikes at t 76, 114, 140 and 170, retrying from the boss
checkpoint after each loss: armour 100 → 93 on the approach, then lost at t 145 (37 s into the fight, the
Spire's bar at ≈ 71 %) and after each of four retries 28–37 s in (the bar at ≈ 81–87 %); no kill in 420 s.

## act-2-outro-capture-r36-a

Round 36, a capture of the game (`.png` sheet only; the sound is in
[level-14-capture-final-r36-a.mp4](level-14-capture-final-r36-a.mp4), t 194–316), from the run above: Level 14
is Act 2's last level, so a won `--level 14` start ends the act without `--act-end`. Eight frames at 75 %:
the daylight with Okafor's closing line (t 206.7–213.9), the debrief (from t 218), the act summary
`ACT II COMPLETE · HOMEFRONT` (from t 250 after Enter; a `--level 14` start records only Level 14),
the four outro pages with their images (voices from t 250.1, 266.1, 280.1 and 294.1: 14.5, 9.4, 10.8 and
9.0 s, `act-complete` under page 1) and the hangar before L15 (`BEFORE L15 - NOT BUILT YET`).

## level-14-capture-daylight-r36-a

Round 36, a supplementary capture of the game, not generated art (`.png` sheet, `.mp4` with the game's sound), taken on
2026-10-12 (step CF) of the step H9 changes, from a snapshot of the uncommitted working tree (10:40), built by `:desktop:installDist` into an isolated build directory, on a private Xvfb display (960×540; settings: a 960×540 window at 0,0), the sound from OpenAL Soft's `wave` driver (`ALSOFT_DRIVERS=wave`, nothing played aloud) aligned at the game's `device reopen` log line, the video `ffmpeg -f x11grab -draw_mouse 0 -framerate 30 -copyts`; **t** is seconds since the game's `[display] window` log line. The clip: H.264 CRF 26, AAC 128 kb/s, the sound 3 dB down, a caption and the level time (0.1 s) burnt in (`drawtext`), the cuts joined with the concat demuxer. The recipe of
[level-14-capture-final-r36-a](#level-14-capture-final-r36-a): `--bench 262 --level 14 --difficulty medium
--loadout front=lance-laser:5,left=micro-missile-pod:5,right=micro-missile-pod:5,rear=tail-gun:2 --special airstrike:3
--invulnerable` (Rook by default), `controls.auto-fire=true`, the same driver (parked under each part's column in the
arena, y 90; Airstrikes at t 76, 114 and 150), no menu presses. **The Spire's kill at t ≈ 194.5** (the flash at 194.4),
the debrief at ≈ 217.5. The video (47 s, 7.7 MB) is two cuts: t 92–113 (ROOTS 8 / 8 at ≈ 94.5, Okafor's root-grid line
≈ 95–104, the supply drone, the Choir ≈ 105, Varga ≈ 111 over the Spire's warning) and 193–219 (the kill, the withering,
the daylight at 25 px/s, the debrief). The sheet: (1) four full frames at 0.44× for the radio; (2) the CDF supply drone at
1:1 (x 140–340 of the play field), t 96.5–103: in from the bottom edge, at its stop ≈ 99.5, the patch released ≈ 100, out
downwards past the ship by ≈ 104; (3) the kill and the daylight at 0.5×, t 194–206 (the beams fall across the stump from
≈ 200); (4) the stump leaving, t 209–217.6: its body off the bottom edge at ≈ 212, the last root tips at ≈ 215.3 (≈ 21 s
after the kill; the H9 estimate was 15 s), nothing of it in the last ≈ 2 s before the debrief.
