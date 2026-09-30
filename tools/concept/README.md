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
