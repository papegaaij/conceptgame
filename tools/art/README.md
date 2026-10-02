# Production art generators

The generators of the **final** sprites, effects, backdrops, HUD parts and recorded sounds (design/art-direction/production). They
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
| `vrell_air.py [skitter] [needler]` | `sprites/skitter_0..95` (24×24, 16 headings × 6 wing-beat frames), `sprites/needler_0..5` (36×36 claw snap) | `design/enemies/air/<slug>/concept/<slug>-final-r12-a` | ~40 s |
| `pulse_cannon.py` | `sprites/pulse-bolt` (14×26), `sprites/pulse-muzzle_0..2` (20×20), `sprites/pulse-impact_0..3` (22×22), additive | `design/player/weapons/pulse-cannon/concept/pulse-cannon-final-r12-a` | ~3 s |
| `enemy_bullets.py` | `sprites/orb_0..3` (15×15 core pulse), `sprites/needle_0..15` (23×23, 16 headings) | `design/enemies/concept/enemy-bullets-final-r12-a` | ~3 s |
| `pickups.py` | `sprites/pickup-{salvage-small,shield-cell,armour-patch,crate}_0..7` (28–34 px loops) | `design/player/concept/pickups-final-r12-a` | ~3 s |
| `explosions.py` | `sprites/explosion-{tiny,small,medium,large}_N` (24/40/64/96 px, 12/12/14/14 frames, additive) | `design/art-direction/concept/explosions-final-r12-a` | ~5 s |
| `loot_targets.py` | `sprites/cargo-container_0..1` (32×24), `sprites/cargo-container-break_0..7` (48×48), `sprites/beacon_0..3` (12×12), `sprites/glint_0..2` (9×9, additive) | `design/campaign/act-1-first-contact/level-01-break-at-dawn/concept/loot-targets-final-r12-a` | ~30 s |
| `backdrop_l01.py [id ...]` | `backdrop/level-01/<id>.png` (`<id>_<n>.png` for frames and headings): every tile set and set piece of Level 01's `backdrop` data at its size, 12–32 colours per piece; the `-mirrored` pieces are the mirrored placements rendered with the layout mirrored and the key light fixed | `design/campaign/act-1-first-contact/level-01-break-at-dawn/concept/backdrop-final-r12-a` | ~1.5 min |
| `hud.py` | `sprites/hud/panel-left`, `panel-right` (240×540 side-panel plates: chamfer, four corner rivets, brushed steel, ordered-dithered face, 26 colours), `plate` (120×22 label plate), `well.9`, `portrait.9` (32×32 and 76×76 LCD and portrait wells, 6 px corners), `fill.9` (8×8 grey phosphor cell, tinted at runtime), `glow.9` (24×24 white readout glow, stepped alpha) | `design/ui/hud/concept/hud-final-r13-a` (sheet only: the pieces are static) | ~30 s |
| `sfx_originals.py [--check] [name ...]` | `sfx/<concept-name>.ogg`: the 75 chosen recorded sounds rebuilt from the Freesound originals (`~/.cache/terran-vanguard/freesound/`, filled by `tools/concept/audio/freesound_fetch.py --download`) with their unchanged `import_sfx.py` settings, originals above full scale clipped first; `SOURCE` comment; then a check against the chosen concept files (length, peak, band RMS within 1 dB) | none (round 12's listening table) | ~1 min |

All of them: `for s in stormhawk vrell_air pulse_cannon enemy_bullets pickups explosions loot_targets backdrop_l01 hud sfx_originals; do python3 tools/art/$s.py; done`
(about 4.5 min). Afterwards `./gradlew check` packs the atlases and checks the budgets.

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
- **Sounds** (`sfx_originals.py`): every OGG gets a `SOURCE` Vorbis comment; `importPlaceholders`
  keeps an OGG that has one (`vanguard.pipeline.PlaceholderSounds`).
- **Review**: `review_sheet` and `save_review` build the sheet and GIF from the files in
  `assets/` (not from the in-memory renders), so the review shows what the game loads.
