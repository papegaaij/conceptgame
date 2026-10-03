# Level 04 – generator notes and prompts

## backdrop-final-r17-a

Round 17, production art proposal (M4 part D). Review sheet made from the final files in
`assets/backdrop/level-04/` by `tools/art/backdrop_l04.py --review`; the pieces are rendered by
`tools/art/backdrop_l04.py` from the level's `backdrop` block in `../data.yaml` (with the `road`
curve and the sections' tile sets; the proposal file they came from was merged into it in part D's
step 3).
Built from the chosen Luna scene (`tools/concept/scenes_r06.py`, scene-luna-r06-a: its regolith with
fbm craters, hillshade and long low-sun cast shadows, the grey ramp, the blue earthshine tint in the
shadows, the dust tones of the plumes, the rail, the domes and the Vrell roots) and Level 01's
station kit (`tools/art/backdrop_l01.py`). Five ground tile sets on one shared terrain and palette
(grey mare in two variants, the rille rims with the channel open onto the far rille floor, the
crater field with teal roots, the mass-driver field with the rail), regolith plumes in three
densities, ejected-rock streaks; set pieces for Tranquility Base (the convoy apron, two pressure
dome clusters, the Apollo 11 heritage dome with the Eagle's gold descent stage under a ribbed glass
dome, landing pads), abandoned rovers, boulders and boulders with a Vrell growth socket (the Spine
Turrets' bases), the rille's two ends over the section seams, the road bridge turned to the road,
split pod-lander husks in their craters, the mass driver's loading station, the sled overlay (30
frames at 10 fps: lamps blinking, faster before the shot, then the sled racing up the rail) and the
terminal gate with static landing lights; the road ribbon's texture (56×192). The sheet ends with
composites of the level at eleven times (half scale), drawn as the game's Backdrop draws the layers,
with the road ribbon and the convoy (civilian crawler frames) on it.

## dugout-r17-a

Round 17, concept mockup (M4 part D): the prospector's dugout, the hardened ground target that
releases the prospector's cache, made by `tools/concept/ground_targets_r17.py` (top-down SDF render,
the fixed top-left key light, the station kit's materials in Level 01's muted kit palette `KIT_PAL`
plus the loot target's matte amber under black hazard stripes, 1 px light rim; intact, damaged and
wrecked frames on a strip of the round-06 Luna regolith at 1× and 3×). Variant A: a corrugated
hut half-buried in a regolith berm with sandbag rows, an armoured hatch plate and a roof band in
amber/black hazard stripes, the far end collapsed under a regolith slide, a mast with a red lamp.

Brief: `top-down late-90s pre-rendered CGI game sprite, 48x32 px, a small hardened lunar prospector's
shelter: a corrugated steel half-cylinder hut dug into a grey regolith berm with sandbag rows, one end
collapsed under a slide of regolith, an armoured hatch plate with dull matte amber and black hazard
stripes, a thin radio mast with a red lamp; key light top-left, short shadow down-right; plus a
damaged frame (dents, scorch marks) and a wrecked frame (roof blown open onto a dark pit, curled ribs,
the hatch thrown aside, everything charred)`.

## dugout-r17-b

Round 17, concept mockup, as dugout-r17-a. Variant B: a low octagonal bunker of sintered regolith
blocks with an armoured roof hatch set in an amber/black hazard ring, a vent stack, a small solar
panel and rubble slid over one corner.

Brief: `top-down late-90s pre-rendered CGI game sprite, 48x32 px, a low octagonal lunar bunker of
sintered grey-beige regolith slabs in a shallow berm, a square armoured roof hatch in a dull matte
amber and black hazard-striped ring, a vent stack, a small blue solar panel, a heap of rubble over one
corner; key light top-left; plus a damaged frame (cracks, the panel knocked askew, scorch) and a
wrecked frame (blown open at the hatch into a dark pit, slab pieces tilted, charred)`.

## supply-drop-r17-a

Round 17, concept mockup (M4 part D): the CDF supply drop, the 3 HP ground target that breaks to
drop medium salvage and a special charge, made by `tools/concept/ground_targets_r17.py` as
dugout-r17-a. Variant A: a steel drop crate with amber/black hazard end bands, lid ribs, a red lamp
and four corner retro nozzles.

Brief: `top-down late-90s pre-rendered CGI game sprite, 32x24 px, a military supply drop crate
landed on the Moon: blue-grey steel body with lid ribs, dull matte amber and black hazard bands at
both ends, four small retro-rocket nozzles at the corners, a red lamp; key light top-left; plus a
damaged frame (dented lid, a nozzle torn off, scorch) and a wrecked frame (split open, the sides
splayed, an empty dark hold)`.

## supply-drop-r17-b

Round 17, concept mockup, as supply-drop-r17-a. Variant B: a squat cylindrical drop pod on four
splayed legs with two amber/black hazard rings, a nose cone, a hatch strip and a red lamp.

Brief: `top-down late-90s pre-rendered CGI game sprite, 32x24 px, a squat cylindrical supply drop
pod lying on four splayed landing legs on lunar regolith, steel body with two dull matte amber and
black hazard-striped rings, a dark nose cone, a hatch strip, a red lamp; key light top-left; plus a
damaged frame (a dent, a leg snapped, scorch) and a wrecked frame (split along the top, the nose cone
knocked off and charred)`.

## level-04-capture-final-r17-a

A capture of the game, not generated art: `desktop/build/install/terran-vanguard/bin/terran-vanguard
--bench 115 --settings <file> --level 4 --invulnerable --special airstrike:2 --debug-speed 2` under
`xvfb-run -s "-screen 0 960x540x24"` (settings: a 960×540 window at 0,0, `audio.master=0`,
`controls.auto-fire=true`), recorded with `ffmpeg -f x11grab -draw_mouse 0` at 4 fps (2 frames per
level second at double speed), eight whole-window frames in a 2 × 4 grid, 8 px apart on `#0b0e14`.
One run, the ship left at its start under the road, the Airstrike never called: the launch with the
convoy rolling in (t≈5.5), the first Brood Pod (t≈23.5) and its burst into six Skitters one frame
later, the t=40 turret nest (t≈42), the intro Scuttler on the rille's west rim, its fan just fired
(t≈76.5), the convoy on the road bridge (t≈106), the heavy lander plume (t≈133), the column at the
terminal gate (t≈186.5).

## l04-targets-final-r17-b

Production sprites (M4 part D batch, round 17), not a concept: `python3 tools/art/l04_targets.py`
(`--review` rebuilds only this sheet and its GIF) renders the chosen variant-a models of
`tools/concept/ground_targets_r17.py` (imported unchanged) at the quality bar's 8× into
`assets/sprites/`: `dugout_0..2` and `supply-drop_0..2` (intact, damaged, wrecked; the concept's
light rim and scorch) and `dugout-break_0..7` / `supply-drop-break_0..7` (72×72 / 48×48: the
damaged model cut into six pieces that fly apart, tumble and char, each frame its own render, the
last three crumbling away as the cargo container's in `tools/art/loot_targets.py`). One 32-colour
palette per target. The GIF plays both: intact, two hits, the break-apart over the wreck that stays.

## level-04-capture-final-r17-b

A capture of the game after round 17's feedback, not generated art, made as level-04-capture-final-r17-a
(`--bench 112`, the same options and settings, `ffmpeg -f x11grab` at 4 fps), six whole-window
frames in a 2 × 3 grid, 8 px apart on `#0b0e14`. One run, the ship left at its start, the
Airstrike never called: the supply drop beside the road (t≈53), the convoy on the road bridge, the
lead crawler passing under the near arch with the arch's shadow on it (t≈103), the column on the
deck with a Spine Turret standing on each arch (t≈104), the dugout in the crater field (t≈122),
the column at the terminal gate (t≈185) and the crawlers driving in under the blockhouse with the
road ending there (t≈187).

## level-04-capture-final-r17-c

A capture of the game after the hangar redo (round 17 item 9), not generated art, made as
level-04-capture-final-r17-b (`--bench 104`, the same options and settings, `ffmpeg -f x11grab` at
4 fps), six whole-window frames in a 2 × 3 grid, 8 px apart on `#0b0e14`. One run, the ship left at
its start, no special fitted: the terminal's vehicle hangar entering the screen with the column on
its apron (t≈185.7), Crawler One at the door (t≈186.7), the column driving in under the roof and
vanishing inside (t≈187.7, t≈188.7), the last crawler in the door bay (t≈189.2) and all crawlers
inside (t≈189.7). One crawler was lost on the way (4 / 5 home), so four drive in.
