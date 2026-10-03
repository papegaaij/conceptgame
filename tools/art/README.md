# Production art generators

The generators of the **final** sprites, weapon effects, backdrops, HUD parts, glass UI kit, screen scenes, equipment icons, portraits, intel pictures, briefing images, bitmap fonts, themes and recorded sounds (design/art-direction/production). They
write straight into `assets/`, which the game packs into atlases at build time, and render the
review material of a batch into the parts' `concept/` directories. Every output is fully
determined by the code (fixed seeds, no hand edits): change the generator and rerun it.

Requirements as for [`tools/concept/`](../concept/README.md): Python 3 with Pillow, numpy and
PyYAML, `ffmpeg` for the review GIFs. Run from anywhere; paths come from `render/config.py`.
The generators import the concept `render/` package (the SDF ray-marcher) and the concept
scripts that made the chosen looks (`vfx_r08.py`, `vfx_r09.py`, `enemies_r04.py`,
`ground_targets.py`) unchanged; those stay frozen.

## Scripts

Each script writes its part's assets and then its review sheet and GIF
(`<subject>-final-rRR-a.png/.gif`); `--review` rebuilds only the review files from `assets/`.
Run times on a 20-core machine.

| Script | Outputs in `assets/` | Review files | Time |
|---|---|---|---|
| `stormhawk.py` | `sprites/ship_0..4` (48×48 banking frames, ±15° and ±30° of roll through a perspective camera, 32 colours), `sprites/pod-<type>-<left\|right>_0..4` (the five wing-pod types per banking frame), `sprites/engine-flame_0..8` (12×18, 3 lengths × 3 frames, additive), `pivots/ship.json`, `pivots/pods.json` | `design/player/ship/concept/player-ship-final-r12-a` | ~40 s |
| `vrell_air.py [skitter] [needler] [stinger]` | `sprites/skitter_0..95` (24×24, 16 headings × 6 wing-beat frames), `sprites/needler_0..5` (36×36 claw snap), `sprites/stinger_0..27` (40×40, 7 tilts ±30° × 4 wing-beat frames), `sprites/stinger-flare_0..2` (additive) | `design/enemies/air/<slug>/concept/<slug>-final-r12-a` (Stinger: `-r15-a`) | ~50 s |
| `vrell_ground.py` | `sprites/spine-turret_0..31` (40×40, the whole turret at 32 barrel headings), `sprites/spine-turret-stump` | `design/enemies/ground/spine-turret/concept/spine-turret-final-r15-a` | ~20 s |
| `vrell_l03.py [bomber] [seed] [debris]` | `sprites/spore-bomber_0..3` (72×72 idle loop, the gas bag breathing), `sprites/spore-mine_0..3` (14×14 lime pulse, additive), `sprites/whirl-seed_0..7` (26×26, six-blade spinner, 7.5° per frame over the 60° loop), `sprites/debris-large-{a,b,c}` (96×80, 72×64, 56×48 wreck chunks of the station kit), `sprites/debris-small-{a,b}` (32×28, 24×24) | `design/enemies/air/{spore-bomber,whirl-seed}/concept/<slug>-final-r16-a`, `design/world/earth-orbit/concept/debris-final-r16-a` (sheet only) | ~15 s |
| `vrell_l04.py [pod] [scuttler]` | proposal, pending part D's doc gaps (parameters at the top of the script): `sprites/brood-pod_0..7` (64×64 pulse loop, the veins brightening with the swell), `sprites/brood-pod-burst_0..11` (96×96 wet burst, additive) and `brood-pod-tatters_0..11` (96×96 membrane and rib pieces, solid), 4 steps per frame; `sprites/scuttler_0..95` (64×64, 16 headings × 6 walk frames, indexed heading * 6 + frame, 24 px per cycle), `sprites/scuttler-husk_0..15` (the legless shell per heading) and `scuttler-glow_0..95` (the lime back's emission, additive, same order) | `design/enemies/{air/brood-pod,ground/scuttler}/concept/<slug>-final-r16-a` | ~10 min (on a loaded machine) |
| `civilian_crawler.py` | `sprites/civilian-crawler_0..20` (72×84 canvas, the 40×72 crawler driving up the screen: 7 headings −30° to +30° clockwise from straight up × 3 wheel frames, indexed heading × 3 + phase, index 0 = −30°, nose to the left; 40 colours), `sprites/civilian-crawler-wreck_0..6` (the burnt-out wreck per heading, 32 colours), `sprites/civilian-crawler-pip` (10×18 HUD pip, flat white, tinted at runtime) | `design/allies/concept/civilian-crawler-final-r17-a` | ~25 s |
| `airstrike_bomber.py` | `sprites/airstrike-bomber_0..3` (56×64, the Airstrike's CDF bomber facing up, 4 frames of engine-flame flicker, 40 colours), `sprites/airstrike-bomber-shadow` (56×64, soft near-black silhouette at up to 50 %, drawn as is offset on the ground) | `design/player/specials/concept/airstrike-bomber-final-r17-a` | ~5 s |
| `leviathan.py` | `sprites/leviathan-down_0..2` (300×480 body of the second pass, 3 tail-sway frames), `sprites/leviathan-vent-1..4`, `leviathan-fin-{left,right}`, `leviathan-fluke_0..2` and a `-wrecked` sprite of each (the shootable parts cut from whole-unit renders by an ID pass), `sprites/leviathan-blowhole-glow` (56×56, additive), `sprites/leviathan-cross_0..2` (the whole unit on its diagonal first pass, 1.25×), `pivots/leviathan.json` (the fluke's centre per sway frame); 48 colours over the second-pass set | `design/enemies/space/leviathan/concept/leviathan-final-r16-a` | ~12 min |
| `vrell_fx.py [bomber] [seed] [ichor]` | the Level 03 Vrell death effects played with their explosion-ladder burst: `sprites/spore-bomber-death_0..15` (96×96 lime spore cloud, additive) and `spore-bomber-tatters_0..15` (96×96 membrane tatters, solid), 15 fps; `sprites/whirl-seed-husk_0..11` (40×40 husk split, solid) and `whirl-seed-death_0..7` (32×32 teal glint, additive), 30 fps; `sprites/leviathan-ichor_0..15` (160×160 ichor cloud, additive, 10 fps, one per wound); the solid pieces ray-marched per frame from the units' models | `design/enemies/air/{spore-bomber,whirl-seed}/concept/<slug>-death-final-r16-a` (the ichor is reviewed in the Leviathan's death, `leviathan_death.py`) | ~1.5 min |
| `leviathan_death.py [--data \| --review]` | the Leviathan's break-up at its death: `sprites/leviathan-chunk-1..5_0..2` (the second-pass body with every part wrecked cut into five chunks along jagged seams, charred hide and violet-glowing torn flesh along the cuts; head, the middle split along the spine left and right, the back, the tail; frame 0 in place, frames 1–2 tumbling about the chunk's centre, each a render under the fixed key light; one 48-colour palette) and the `death` entry of `pivots/leviathan.json` (the chunks' centres per frame and drift, the swap, sinking, darkening and fade, and the blast table the game plays: explosion-large/-medium and leviathan-ichor by step and offset); `--data` rewrites only the data and the review | `design/enemies/space/leviathan/concept/leviathan-death-final-r16-b` | ~7 min |
| `voice.py [--keep\|--list]` | `voice/<voice>/<key>.ogg`: every spoken radio line and briefing page (design/audio/voice), Chatterbox in `~/.cache/tv-tts/venv-chatterbox` with the line list from `./gradlew :pipeline:voiceLines`; renders only the missing lines and deletes unused files | none (reviewed in a concept round) | ~15 min for Act 1 on an RTX 2070 |
| `pulse_cannon.py` | `sprites/pulse-bolt` (14×26), `sprites/pulse-muzzle_0..2` (20×20), `sprites/pulse-impact_0..3` (22×22), additive | `design/player/weapons/pulse-cannon/concept/pulse-cannon-final-r12-a` | ~3 s |
| `weapon_fx.py` | the Act 1 arsenal's effects: `sprites/<weapon>-shot_<deg>` for the Scatter Vulcan and Autocannon (tracers) and the Side Splitter (bolts), one per angle their patterns use, `sprites/lance-laser-shot_1..6` (per level, 6 = overdrive), `sprites/micro-missile-pod-shot_0..31` (24×24, 32 headings with the plume), `sprites/bomb-rack-shot`, `hammer-mortar-shot` (solid), `sprites/ballistic-muzzle_0..2`, `launcher-muzzle_0..2` (20×20), `sprites/ballistic-impact_0..3`, `explosive-impact_0..3` (additive) | `design/player/weapons/concept/weapons-final-r14-a` | ~4 s |
| `enemy_bullets.py` | `sprites/orb_0..3` (15×15 core pulse), `sprites/needle_0..15` (23×23, 16 headings) | `design/enemies/concept/enemy-bullets-final-r12-a` | ~3 s |
| `pickups.py [name ...]` | `sprites/pickup-{salvage-small,shield-cell,armour-patch,crate,salvage-medium,overdrive,salvage-large}_0..7` (28–35 px loops; salvage L is seven credit chips, a raised centre chip in a ring of six) | `design/player/concept/pickups-final-r12-a` (salvage M and overdrive: `-r15-a`; salvage L: `-r16-a`); with names only those pickups and their batch's review files | ~3 s |
| `explosions.py` | `sprites/explosion-{tiny,small,medium,large}_N` (24/40/64/96 px, 12/12/14/14 frames, additive) | `design/art-direction/concept/explosions-final-r12-a` | ~5 s |
| `loot_targets.py` | `sprites/cargo-container_0..1` (32×24), `sprites/cargo-container-break_0..7` (48×48), `sprites/beacon_0..3` (12×12), `sprites/glint_0..2` (9×9, additive) | `design/campaign/act-1-first-contact/level-01-break-at-dawn/concept/loot-targets-final-r12-a` | ~30 s |
| `l04_targets.py` | Level 04's loot targets from the chosen round 17 models (`tools/concept/ground_targets_r17.py`, variant a): `sprites/dugout_0..2` (48×32) and `sprites/supply-drop_0..2` (32×24), intact, damaged, wrecked; `sprites/dugout-break_0..7` (72×72) and `sprites/supply-drop-break_0..7` (48×48), the break-apart as the cargo container's; one palette per target | `design/campaign/act-1-first-contact/level-04-tranquility-run/concept/l04-targets-final-r17-b` | ~1 min |
| `backdrop_l01.py [id ...]` | `backdrop/level-01/<id>.png` (`<id>_<n>.png` for frames and headings): every tile set and set piece of Level 01's `backdrop` data at its size, 12–32 colours per piece; the `-mirrored` pieces are the mirrored placements rendered with the layout mirrored and the key light fixed; with ids only those pieces (and the sheets of the reworked ones among them) | `design/campaign/act-1-first-contact/level-01-break-at-dawn/concept/backdrop-final-r12-a`; pieces reworked after round 12 (`REWORKED`: `north-arm`, its solar wings end inside the piece) get their own `<id>-final-r13-a` | ~1.5 min |
| `backdrop_l02.py [id ...]` | `backdrop/level-02/<id>.png`: every tile set and set piece of Level 02's `backdrop` data, Level 01's production pieces for the shared station kit plus the burning north arm, the docks with their growth, the *Resolute*, smoke and coolant banks, frost streaks, open space | `design/campaign/act-1-first-contact/level-02-shipyard-burning/concept/backdrop-final-r15-a` | ~1 min |
| `backdrop_l03.py [id ...]` | `backdrop/level-03/<id>.png`: every tile set and set piece of Level 03's `backdrop` data, the high orbital lanes over the first battle's wreckage: Earth with two weather fronts, lane beacons and buoys, the orbital defence ring, the broken frigate *Kestrel*, CDF platform halves, wreck plates and a burnt-out tug, spore haze and banks, wisps, frost and spore streaks (Level 01's and 02's production pieces for the shared kit), `lifeboat-light` (12×12, additive) | `design/campaign/act-1-first-contact/level-03-spore-drift/concept/backdrop-final-r16-a` (sheet only) | ~1 min |
| `backdrop_l04.py [id ...]` | `backdrop/level-04/<id>.png` (`sled-run_0..29`): every tile set and set piece of Level 04's `backdrop` block (its data.yaml), the Luna surface from the chosen scene (scenes_r06): five ground tile sets on one shared terrain and palette, so section seams vanish (grey mare ×2, the rille rims with a transparent channel, the crater field with Vrell roots, the mass-driver field with the rail), the far rille floor, regolith plumes, ejected-rock streaks, Tranquility Base with the Apollo 11 heritage dome, rovers, boulders and turret sockets, the rille's ends (cut from the tiles' own render over the seams), the road bridge turned to the road (`road-bridge` the deck, `road-bridge-arches` its overhead arches), pod-lander husks, the rail head, the sled overlay, the terminal's vehicle hangar (`terminal-gate` the apron, `terminal-gate-roof` the overhead 380×300 hangar); `road-texture` (56×192, the road ribbon's texture, contract in the script) | `design/campaign/act-1-first-contact/level-04-tranquility-run/concept/backdrop-final-r17-a` (sheet only, with composites of the level) | ~5 min (the hangar alone ~4 min on one core) |
| `crane_four.py` | `sprites/crane-four-arm_0..60` (the arm at 61 angles, 3° apart, under the fixed key light), `pivots/crane-four.json` (the pivot per frame, the lights along the jib), `sprites/crane-four-light`, `crane-four-clamp-light` (additive), `crane-four-canister` | `design/campaign/act-1-first-contact/level-02-shipyard-burning/concept/crane-four-final-r15-a` | ~2 min |
| `hud.py` | `sprites/hud/panel-left`, `panel-right` (240×540 side-panel plates: chamfer, four corner rivets, brushed steel, ordered-dithered face, 26 colours), `plate` (120×22 label plate), `well.9`, `portrait.9` (32×32 and 76×76 LCD and portrait wells, 6 px corners), `fill.9` (8×8 grey phosphor cell, tinted at runtime), `glow.9` (24×24 white readout glow, stepped alpha) | `design/ui/hud/concept/hud-final-r13-a` (sheet only: the pieces are static) | ~30 s |
| `ui_kit.py` | `sprites/ui/`: the glass kit of the out-of-game screens as nine-patches (`glass.9` body, `frame.9` / `frame-on.9` / `dialog.9` trims, `inset.9`, `rule.9`, `selection.9`, `chip.9` / `chip-on.9` / `chip-off.9`, `tab.9` / `tab-on.9`, `bar.9` grey cell, `row.9`, `hint.9`, `tag.9` grey, `callout.9` / `callout-on.9`) and pieces (`cursor-small`, `cursor`, `cursor-large`, `knob`, `diamond`, `chevron` grey, `scroll-up`, `scroll-down`) | `design/ui/concept/ui-kit-final-r13-a` | ~2 s |
| `ui_scenes.py` | `ui/title-scene.png` (960×540 hero scene of main menu A), `ui/title-logo.png` (logo D, 460 px, stepped translucency), `ui/hangar-map.png` (960×540 tactical map, Act 1); textures of their own, not packed | `design/ui/main-menu/concept/main-menu-final-r13-a` | ~15 s |
| `icons.py` | `sprites/icons/<name>.png` (16×16) and `<name>-large.png` (24×24): one per shop item of the catalogue (weapon slug, or `<kind>-<name as a slug>`) and `escort-rook` | `design/ui/hangar/concept/hangar-final-r13-a` (with the map) | ~15 s |
| `ui_review.py` | none: arranges the game captures `<subject>-capture-final-r13-a.png` of the screens without art of their own | `design/ui/{briefing,debrief,pause}/concept/<subject>-final-r13-a` | ~2 s |
| `portraits.py` | `sprites/portraits/radio-<speaker>-<expression>` (72×72) and `briefing-<speaker>-<expression>` (144×144, the 72 px portrait at 2× with CRT scanlines): Okafor, Rook, Varga in `neutral`, `grim`, `fierce` (briefing size for Okafor and Varga), `generic-cdf` and `generic-civilian` neutral, `radio-the-choir-neutral_0..31` (the glyph loop, 12 fps); one 36-colour figure palette per character | `design/story/characters/concept/portraits-final-r13-a` | ~30 s |
| `intel.py [name ...]` | `sprites/intel/<enemy>` (30×30 sensor portraits of the waves' enemy types: skitter, needler, stinger, spine-turret; spore-bomber, whirl-seed, the latter the six-blade seed of `vrell_l03.py`; brood-pod, scuttler from `vrell_l04.py`) and `boss-<boss>` (40×40 silhouettes: gorgon-frigate, brood-carrier); set pieces (the Leviathan) get none; with names only those pictures | `design/ui/hangar/concept/intel-final-r13-a` (the Level 03 portraits: `-r16-a`, Level 04's: `-r17-a`; sheets only) | ~20 s |
| `briefing_images.py [name ...] [r13\|r20] [--review]` | `ui/briefing/<name>.png` (672×240, one per briefing page of the Act 1 intro and Levels 01–04; textures of their own, not packed); shows the sprites of `stormhawk.py`, `vrell_air.py`, `intel.py`, `vrell_l03.py`, `leviathan.py`, `vrell_l04.py`, `civilian_crawler.py` and `airstrike_bomber.py` | `design/ui/briefing/concept/briefing-images-final-r20-a` (sheet only; `r13` rewrites the round-13 sheet `briefing-images-final-r13-a`) | ~10 s |
| `fonts.py [--review] [--check]` | `fonts/label-8x12`, `body-10x20`, `heading-20x30` (`.fnt` BMFont text + one `.png` page each; DejaVu Sans Mono Bold at 9/15/26 px, hinted 1-bit, white, cell-width advance, baselines 9/15/24, 127 characters); `--check` fails on any character in the data files or the game's string literals without a glyph | `design/ui/concept/fonts-final-r13-a` (sheet only) | ~2 s |
| `themes.py [title] [hangar] [briefing] [--check] [--review]` | `music/title-theme.ogg`, `hangar-theme.ogg`, `briefing-theme.ogg`: the chosen themes rendered by their generator (`music_r08.py` `render_loop`, unchanged), OGG Vorbis q6, −14 LUFS, intro + loop + fade tail with `LOOPSTART` / `LOOPLENGTH`, remuxed with a `SOURCE` comment; then a check (loudness ±0.5 LU, true peak, q6, loop tags, seam, identical to the chosen file) | `design/audio/music/concept/themes-final-r13-a` (sheet only) | ~6 min |
| `sfx_originals.py [--check] [name ...]` | `sfx/<concept-name>.ogg`: the 75 chosen recorded sounds rebuilt from the Freesound originals (`~/.cache/terran-vanguard/freesound/`, filled by `tools/concept/audio/freesound_fetch.py --download`) with their unchanged `import_sfx.py` settings, originals above full scale clipped first; `SOURCE` comment; then a check against the chosen concept files (length, peak, band RMS within 1 dB) | none (round 12's listening table) | ~1 min |

All of them: `for s in stormhawk vrell_air vrell_ground vrell_l03 civilian_crawler airstrike_bomber leviathan pulse_cannon weapon_fx enemy_bullets pickups explosions vrell_fx leviathan_death loot_targets l04_targets backdrop_l01 backdrop_l02 backdrop_l03 crane_four hud ui_scenes ui_kit icons ui_review portraits intel briefing_images fonts themes sfx_originals; do python3 tools/art/$s.py; done`
(about 30 min, the Leviathan alone ~12; `ui_scenes` before `ui_kit` and `icons`, whose sheets show its scene and map;
`stormhawk`, `vrell_air` and `intel` before `briefing_images`, which shows their sprites;
`vrell_l03`, `leviathan` and `explosions` before `vrell_fx`, whose review plays its effects with their units and bursts; `vrell_fx` before `leviathan_death`, whose review plays the ichor). Afterwards `./gradlew check` packs the atlases and checks the budgets.

## Conventions (`artkit.py`)

- **Render path** (art direction, render pipeline): SDF render at 8× for sprites up to 64 px
  and 4× above → box downsample → 1-bit alpha at 50 % coverage → unsharp mask → **one**
  median-cut palette (24–48 colours, no dithering) for all frames of a sprite, so frames never
  flicker between palettes. Effects (shots, flashes, explosions, flames) are the chosen 2D light
  fields, analytic and supersampled 4×.
- **Additive effects** are stored premultiplied on black with alpha 1 wherever they add light:
  what the game's `GL_SRC_ALPHA, GL_ONE` blend adds.
- **Glow halos** around a solid body (bullets, pickups) keep the body at 1-bit alpha and step the
  soft halo to four translucency levels (`stepped_alpha`); `ordered_dither` (4×4 Bayer) is for
  wide gradients (the backdrops).
- **Names**: the frames the game loads, `name_<i>.png` (an indexed atlas region) or `name.png`
  for a single region. Angle sets (`angle_set`) are indexed `heading × phases + phase`, heading
  0 = moving down the screen, turning clockwise; every heading is its own render with the key
  light fixed, nothing lit is rotated or mirrored as an image. `write_frames` removes the old set
  of that name first, so a changed frame count leaves no stale frames.
- **Perspective** (`perspective`): renders are orthographic top views, but the Stormhawk's banking
  frames are seen through a camera 4 model units up, so a roll reads (the raised wing grows, the
  lowered one shrinks); its mount points in `ship.json` are projected the same way.
- **Pivot files**: `assets/pivots/<sprite>.json`, per frame the points other sprites attach to
  (px from the sprite's top left), with a `source` entry. `ship.json`: the mount points of
  `design/player/ship/data.yaml` rolled with each banking frame; `pods.json`: each pod sprite's
  top-left offset on the hull per banking frame and the flame's attach point.
- **Final marker**: every PNG gets a `Source` text chunk (`tools/art/<script> (production art,
  ...)`). The build's `importPlaceholders` skips a part whose first frame has one
  (`vanguard.pipeline.FinalArt`); placeholders carry none or a `Placeholder` chunk.
- **Backdrops** (`backdrop_l01.py`): built from the placeholder generator's kit, layouts and seeds
  (`tools/concept/backdrop_l01.py`, loaded by path since it has the same name), each piece
  posterized on its opaque pixels to 12–32 colours with one palette per frame set; wide gradients
  ordered-dithered; translucent smoke and sparks stepped. Nothing lit is mirrored at runtime: a
  mirrored placement gets its own `-mirrored` piece.
- **HUD parts** (`hud.py`): in `assets/sprites/hud/`, packed onto the shared sprite pages as
  `hud/<name>`. A part that stretches is a libGDX nine-patch, `<name>.9.png`: the image inside a
  1 px border whose black marks on the top and left edges are the stretched columns and rows
  (`AtlasPacker` turns them into the region's `split`); nothing that stretches is dithered or
  textured, so a stretched edge stays clean. The side panels never stretch (the screen is a fixed
  960×540) and are whole plates. Solid parts are ray-marched with the key light lower than the
  sprites' (so a 45° bevel reads) and mapped by their shading through palette B's UTC HULL ramp;
  the fill and glow are grey/white and tinted at runtime.
- **Glass UI kit** (`ui_kit.py`): in `assets/sprites/ui/` as `ui/<name>`. A panel is two
  nine-patches, the glass body (opaque in the file, drawn at the panel's opacity) and the trim on
  top, so the opacity never fades the metal. Solid parts are ray-marched at 8× with `hud.py`'s
  light (loaded by path: `tools/concept` has a `hud.py` of its own) and mapped through a ramp
  (UTC HULL steel, the kit's amber, holo cyan); 1 px features are pixel bevels. The selection bar's
  fade sits in its fixed right part, dithered along x only, so the rows a stretch repeats stay
  identical. Grey pieces (`bar`, `tag`, `chevron`) are tinted at runtime. `artkit.chamfered_box` is
  the shared chamfered plate.
- **Screen scenes** (`ui_scenes.py`): composed in layers, each posterized on its own palette (space,
  Earth, the sun; each sprite) with ordered dither on wide gradients; ships are ray-marched at their
  size in the scene and turned in the model (heading), never rotated as images.
- **Equipment icons** (`icons.py`): a bevelled dark steel badge with the item: its model where one
  exists, otherwise a symbol per kind in the kind's colour, the grade as amber studs; the names
  match `vanguard.game.hangar.ItemIcons.name` (weapon slug, or kind and name lower case with
  hyphens).
- **Portraits** (`portraits.py`): the concept busts and the round-04 style B, imported unchanged;
  an expression swaps the concept module's `face` (and the head's `turn`, for the pitch) for the
  time of a render, so the busts' scene code stays the concept's. The style is split at its palette:
  one median-cut palette over all expressions of a character, then the comm screen (outline,
  interference seeded per character, glow, 2× scanlines) on each. The game derives the names from
  the data's speaker (`vanguard.game.render.Portraits`: lower case, spaces to hyphens).
- **Briefing images** (`briefing_images.py`): composed in layers like the hangar map: the display
  with its grid and the planets posterized to 24 colours with ordered dither, the lines, markers and
  labels (the concept pixel font) to 16 of their own, the sprites last with their palettes.
- **Sounds** (`sfx_originals.py`): every OGG gets a `SOURCE` Vorbis comment; `importPlaceholders`
  keeps an OGG that has one (`vanguard.pipeline.PlaceholderSounds`).
- **Review**: `review_sheet` and `save_review` build the sheet and GIF from the files in
  `assets/` (not from the in-memory renders), so the review shows what the game loads.
