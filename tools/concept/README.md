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

Run from the repository root, e.g. `python3 tools/concept/make_all.py`.

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
- `terrain.py` – procedural backgrounds (coastline, Earth from orbit, city streets) and
  `recede()` which pushes a background back so sprites stay readable.
- `raster.py` – tileable value noise / fBm, colour ramps, glows, starfields, the 5x7 bitmap
  font used for sheet labels and HUD text, and presentation-sheet helpers.
