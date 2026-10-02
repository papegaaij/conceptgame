# Concept mockup generators

Reproducible generators for the concept art mockups in `design/`. Every output is fully
determined by the code (fixed seeds), so the files can be regenerated at any time. Do not
hand-edit generated files; change the generator and rerun it. See `CLAUDE.md` for the concept
art rules.

Requirements: Python 3 with Pillow and numpy, plus `ffmpeg` for GIF encoding. Logos use the
DejaVu Sans fonts when present (other system fonts as fallback).

## Scripts (visual, round 01)

| Script | Outputs | Notes |
|---|---|---|
| `player_ship.py` | `design/player/ship/concept/player-ship-r01-{a,b,c}.png` | three AF-12 Stormhawk silhouettes, ~10 s |
| `palettes.py` | `design/art-direction/concept/palette-r01-{a,b,c}.png` | three palette/mood options |
| `parallax.py [a] [b]` | `design/art-direction/concept/parallax-r01-{a,b}.{png,gif}` | orbit and night-city scenes, ~1 min |
| `hud.py` | `design/ui/hud/concept/hud-r01-{a,b}.png` | two HUD styles, reuses parallax scene A |
| `logos.py` | `design/ui/main-menu/concept/logo-r01-{a,b,c,d}.png` | four title/logo candidates |
| `make_all.py` | all of the above | about two minutes |

Run from the repository root, e.g. `python3 tools/concept/make_all.py --round 01`. Round 01
was made at 640x360 and is frozen; rejected outputs are rewritten into `concept/rejected/`.

## Scripts (visual, round 02 – 960x540, palette B)

| Script | Outputs | Notes |
|---|---|---|
| `ships_r02.py` | `design/player/ship/concept/player-ship-r02-a.png`, `design/player/wingmen/concept/rook-craft-r02-{a,b}.png` | ship A at 48x48 and Rook's craft (ship C geometry, 40x40) in two schemes, ~15 s |
| `parallax_r02.py [a] [b] [c]` | `design/art-direction/concept/parallax-r02-{a,b,c}.{png,gif}` | orbit (fuller, faster), calm night city, Mars canyon; 4 s loops at 20 fps, ~1 min |
| `hud_r02.py` | `design/ui/hud/concept/hud-r02-a.png` | HUD A at 960x540, reuses parallax scene A of round 02 |
| `parallax_r03.py [a] [b] [c]` | `design/art-direction/concept/parallax-r03-{a,b,c}.{png,gif}` | round 03 decoration pass: subclasses the round 02 scenes (clouds, fog, dust banks, trees, gardens, greenhouses, lichen); GIFs with reserved bullet colours, ~1 min |
| `make_all.py` | all of the above (default round) | about three minutes |

## Scripts (visual, round 03 – enemies)

| Script | Outputs | Notes |
|---|---|---|
| `enemies_r03.py [slug ...]` | `design/enemies/{air,ground}/concept/<slug>-r03-<v>.png`, `design/enemies/bosses/concept/brood-carrier-r03-a.png`, `design/enemies/concept/lineup-r03-a.png` | Act 1 Vrell set (two design languages for Skitter, Needler, Spine Turret), three Ascendancy units, the Brood Carrier boss and a lineup; models in `render/enemy_models.py`; ~5 min for everything |

## Scripts (visual, round 04 – enemy colour pass)

| Script | Outputs | Notes |
|---|---|---|
| `enemies_r04.py [slug ...]` | `design/enemies/{air,ground,bosses}/concept/<slug>-r04-a.png`, `design/enemies/concept/lineup-r04-a.png` | re-renders the chosen round-03 enemies with the role colours (`ROLE_SCHEMES` etc. in `render/enemy_models.py`) through the round-03 sheet code; Ascendancy rim light; lineup with colour legend; ~4 min |

## Scripts (visual, round 05 – new enemy archetypes)

| Script | Outputs | Notes |
|---|---|---|
| `enemies_r05.py [name ...]` | `design/enemies/{air,ground,space}/concept/<name>-r05-a.{png,gif}`, `design/enemies/concept/size-lineup-r05-a.png` | Coilwyrm, Leviathan, Scuttler, Whirl Seed, Mote Swarm, Warden Tank, Strider, Buzzsaw Drone, Rail Serpent: PNG sheet + GIF in the play field each. Models in `render/archetype_models.py`; rigs (`AngleSprites` at 16/32 headings, segment chains, spline paths, GIF writer) in `render/enemy_rigs.py`. Units render independently, so they can run in parallel processes; ~4 min per unit, lineup ~3 min |

## Scripts (visual, round 05 follow-up – Vrell beasts)

| Script | Outputs | Notes |
|---|---|---|
| `enemies_r05b.py [ravager] [shellback] [size-lineup]` | `design/enemies/ground/concept/{ravager,shellback}-r05-a.{png,gif}`, `design/enemies/concept/size-lineup-r05-b.png` | animal-like Vrell ground units; models in `render/beast_models.py`, rendered with `enemy_rigs.ModelSpaceAngleSprites` (patterns turn with the body). Shares sheet helpers with `enemies_r05.py`; ~10 min per beast |

## Scripts (round 06)

| Script | Outputs | Notes |
|---|---|---|
| `enemies_r06.py [name ...] [lineup]` | `design/enemies/{air,ground,naval}/concept/<name>-r06-a.{png,gif}`, `design/enemies/concept/lineup-r06-a.png` | Creeper, Hive Node, Wraith, Lamprey, Driftjelly, Reef Spitter, Skimmer, Threadcrawler, Halo Platform, Dust Devil, Spiral Nautilus; models in `render/r06_models.py`, rendered with `ModelSpaceAngleSprites` |
| `bosses_r06.py [gorgon] [kraken] [spire]` | `design/enemies/bosses/concept/{gorgon-frigate,harbour-kraken,siege-spire}-r06-a.{png,gif}` | models in `render/boss_models.py` (with the `bounded()` speed-up); ~16 min for all three |
| `scenes_r06.py [luna europa belt jovian vrell-space]` | `design/art-direction/concept/scene-<setting>-r06-a.{png,gif}` | five setting scenes at medium atmosphere; models in `render/scene_models.py`; ~5–8 min per scene |
| `ui_r06.py [menu] [difficulty] [load] [hangar]` | `design/ui/main-menu/concept/{main-menu,difficulty,load-game}-r06-{a,b}.png`, `design/ui/hangar/concept/hangar-r06-{a,b}.png` | main menu, difficulty select, load game and hangar screens; ~35 s |

## Scripts (round 07)

| Script | Outputs | Notes |
|---|---|---|
| `scenes_r07.py [a] [b] [--sheet]` | `design/art-direction/concept/scene-europa-r07-{a,b}.{png,gif}` | Europa under water (kelp, bubbles, caustics, fish); subclasses `scenes_r06.EuropaScene`; ~4 min per GIF |
| `kraken_r07.py` | `design/enemies/bosses/concept/harbour-kraken-r07-a.{png,gif}` | water as a height field, depth-banded submerged parts, top-down surfacing; ~8 min |
| `enemies_r07.py [halo-platform] [driftjelly]` | `design/enemies/{ground,naval}/concept/{halo-platform,driftjelly}-r07-a.{png,gif}` | dense 6-fold angle set for the Halo ring (frames cached outside the repo), water-plane clipping for the Driftjelly; models in `render/r07_models.py` |
| `ui_r07.py [a] [b]` | `design/ui/hangar/concept/hangar-r07-{a,b}.png` | hangar layout B in the menu's glass style; ~16 s |

## Scripts (round 08)

| Script | Outputs | Notes |
|---|---|---|
| `scenes_r08.py [ocean storm arctic geneva luna-farside] [--sheet]` | `design/art-direction/concept/scene-<name>-r08-a.{png,gif}` | Earth scenes with reusable water code (wave surface, wakes, foam); only `ocean` rendered so far; ~3.5 min per scene |
| `vfx_r08.py [ship] [projectiles] …` | `design/player/{ship,weapons}/concept/…-r08-a.*` | combat effects; rendered: ship (5 banking frames, wing pods) and projectiles. Explosions, enemy bullets, pickups, specials, edge warnings and Rook banking are written but not rendered |
| `ui_r08.py [briefing act-title debrief failed gameover pause options credits hud kit]` | `design/ui/*/concept/*-r08-a.png` | remaining UI screens, HUD refresh, UI kit with font specimens; ~1 min |
| `portraits_r08.py [choir cdf civilian]` | Choir glyph (png+gif), generic CDF and civilian portraits | ~20 s |
| `rerender_r08.py [scuttler coilwyrm leviathan]` | `…/<name>-r08-a.{png,gif}` | r05 units re-rendered with `ModelSpaceAngleSprites`; ~5 min each |

## Scripts (round 09)

| Script | Outputs | Notes |
|---|---|---|
| `scenes_r09.py [ocean storm arctic geneva luna-farside] [--sheet]` | `design/art-direction/concept/scene-<name>-r09-a.{png,gif}` | subclasses `scenes_r08`; shared water helpers (broken Kelvin wakes, wash masks, foam) |
| `vfx_r09.py [beam rook bullets explosions pickups specials warnings]` | beam impact, Rook banking, enemy bullets, explosions, pickups, specials, edge warnings (r09 files) | imports `vfx_r08`; adds the CDF bomber model and a fixed fireball (`explosion_frames9`); ~4 min for all |
| `scenes_r10.py [geneva] [storm] [ocean] [--sheet]` | `design/art-direction/concept/scene-<name>-r10-a.{png,gif}` | round 10: Geneva rebuilt as a city; storm and ocean within the motion budget (25 fps sampling of the 80-step clock) |

## Scripts (round 11)

| Script | Outputs | Notes |
|---|---|---|
| `ui_r11.py [a] [b] [c]` | `design/ui/hud/concept/edge-warnings-r11-{a,b,c}.{png,gif}` | three edge-warning looks over a still Level 01 frame built from `assets/`; a few seconds each |
| `backdrop_fixes_r11.py` | `design/campaign/act-1-first-contact/level-01-break-at-dawn/concept/backdrop-fixes-r11-a.{png,gif}` | before (git `acd4d42`, via `git lfs smudge`) / after (`assets/backdrop/level-01/`) of the `backdrop_l01.py` fixes |

## Scripts (game placeholders – level backdrops and ground targets)

| Script | Outputs | Notes |
|---|---|---|
| `backdrop_l01.py [id ...]` | `assets/backdrop/level-01/<id>.png` (frames `<id>_<n>.png`) | Level 01's tile sets and set pieces at the sizes in its `data.yaml` (`backdrop`); built from the chosen Earth orbit scene (`parallax_r02`/`parallax_r03` helpers, `render/station.py` kit, palette B); ~1.5 min |
| `ground_targets.py` | `assets/sprites/cargo-container_<n>.png`, `cargo-container-break_<n>.png`, `beacon_<n>.png`, `glint_<n>.png` | Level 01's loot targets to the art direction's readability rule 7, at the sizes in its `data.yaml` (`ground_targets`); `render/sdf.py` with palette B; a few seconds |

Unlike the concept generators, these write **game placeholders into `assets/`**, not concept
art into `design/`: a chosen scene is a single composed sheet (and the ground targets have no
concept art yet), so the pieces a level is built from are rendered here from the same models and rules instead of being copied by
`:pipeline:importPlaceholders` (see the architecture's *Assets* section). Every PNG carries a
`Placeholder` text chunk naming its script. Rerun it after changing the level's `backdrop` sizes;
the game's `BackdropAssetsTest` fails when an image is missing or has another size.

Audio round 08: `audio/music_r08.py` (cues and full-length tracks) and `audio/sfx_r08.py` (synthesized pickups and UI sounds); round 11: `audio/music_r11.py` (Coalition Rising base stem) and `audio/sfx_r11.py` (launch rail, edge-warning tones); recorded sounds via `audio/import_sfx.py`. The balancing script is `tools/balance.py` (numbers from the parts' `data.yaml` files, purchases from `design/player/balance-plan.yaml`).

## Audio (round 01)

Audio generators live in [`audio/`](audio/README.md): a small numpy synth (`synth.py`), the
generators `sfx.py` and `music.py`, and `analyze.py` for peak/loudness/spectrogram checks.
Outputs go to `design/audio/sfx/concept/` and `design/audio/music/concept/`.

## Review board

`board.py RR` scans `design/**/concept/` for round `RR` files and writes
`design/concept-rounds/round-RR/index.html` (see `design/concept-rounds/README.md`).

## The `render/` package

- `sdf.py` – a small numpy signed-distance-field ray-marcher. Orthographic top-down camera,
  point key light at the top-left, soft shadows, ambient occlusion, Blinn-Phong specular and a
  90s chrome environment reflection. This is what gives sprites the pre-rendered CGI look.
- `models.py` – SDF models: the three player ship variants, Vrell dart / brood / spore turret,
  orbital station segments and debris.
- `sprite.py` – render → native sprite pipeline: box downsample with coverage, 1-bit alpha,
  mild unsharp mask, median-cut palette; plus shadows and clipped pasting.
- `terrain.py` – procedural backgrounds (coastline with palette stops, Earth from orbit, city streets) and
  `recede()` which pushes a background back so sprites stay readable.
- `raster.py` – tileable value noise / fBm, colour ramps, glows, starfields, the 5x7 bitmap
  font used for sheet labels and HUD text, and presentation-sheet helpers.
- `config.py` – **shared screen geometry** (960x540, play field 480x540 at x = 240, 240 px
  panels, ship 48 px, Rook 40 px; round 01 values in `R01`) and `out_path()`.
- `palette.py` – the three round 01 palettes as 6-step ramps; `B` (90s Neon CGI) is the
  reference palette, with helpers for ship and Vrell material colours.
- `station.py` – round 02 kit of pre-rendered UTC structure parts (trusses, drums, solar
  arrays, radiators, dock, dish, turret, cargo, Mars dome) that scenes kit-bash in 2D.
